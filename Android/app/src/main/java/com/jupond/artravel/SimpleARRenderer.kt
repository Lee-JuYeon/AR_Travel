package com.jupond.artravel

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SimpleARRenderer(
    private val context: Context,
    private val session: Session
) : GLSurfaceView.Renderer {

    private val TAG = "SimpleARRenderer"
    private var currentSite: MainActivity.HistoricalSite? = null

    // 렌더링 상태
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var isGlInitialized = false

    // 카메라 텍스처 관련 변수
    private var cameraTextureId = -1
    private var cameraProgram = 0
    private var cameraPositionAttrib = 0
    private var cameraTexCoordAttrib = 0
    private var cameraTextureUniform = 0

    // 화면을 덮는 쿼드를 위한 버퍼
    private lateinit var quadVertices: FloatBuffer
    private lateinit var quadTexCoords: FloatBuffer

    // 모델 관련 변수
    private var modelShaderProgram = 0
    private var modelPositionAttrib = 0
    private var modelNormalAttrib = 0
    private var modelMvpMatrixUniform = 0
    private var modelLightPosUniform = 0
    private var modelColorUniform = 0

    // 모델 데이터와 로더
    private var modelData: ObjModelLoader.ModelData? = null
    private var modelLoader: ObjModelLoader? = null

    // 행렬 변수
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    // 카메라 포즈 행렬 저장용
    private val cameraPoseMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated() 호출됨")
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        try {
            // OBJ 모델 로더 초기화
            modelLoader = ObjModelLoader(context)

            // 카메라 텍스처 생성
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            cameraTextureId = textures[0]

            Log.d(TAG, "카메라 텍스처 ID: $cameraTextureId")

            // ARCore에 텍스처 ID 전달
            session.setCameraTextureName(cameraTextureId)

            // 카메라 텍스처 설정
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            // 카메라 셰이더 초기화
            val vertexShader = """
                attribute vec4 a_Position;
                attribute vec2 a_TexCoord;
                varying vec2 v_TexCoord;
                void main() {
                    gl_Position = a_Position;
                    v_TexCoord = a_TexCoord;
                }
            """.trimIndent()

            val fragmentShader = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES u_Texture;
                varying vec2 v_TexCoord;
                void main() {
                    gl_FragColor = texture2D(u_Texture, v_TexCoord);
                }
            """.trimIndent()

            // 셰이더 프로그램 생성
            val vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
            val fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

            cameraProgram = GLES20.glCreateProgram()
            if (cameraProgram == 0) {
                throw RuntimeException("프로그램 생성 실패")
            }

            GLES20.glAttachShader(cameraProgram, vertexShaderId)
            GLES20.glAttachShader(cameraProgram, fragmentShaderId)
            GLES20.glLinkProgram(cameraProgram)

            // 프로그램 연결 상태 확인
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(cameraProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(cameraProgram)
                GLES20.glDeleteProgram(cameraProgram)
                throw RuntimeException("셰이더 프로그램 링크 실패: $log")
            }

            // 셰이더 속성 및 유니폼 위치 가져오기
            cameraPositionAttrib = GLES20.glGetAttribLocation(cameraProgram, "a_Position")
            cameraTexCoordAttrib = GLES20.glGetAttribLocation(cameraProgram, "a_TexCoord")
            cameraTextureUniform = GLES20.glGetUniformLocation(cameraProgram, "u_Texture")

            // 화면을 덮는 쿼드 정점 데이터 초기화
            val quadVerticesData = floatArrayOf(
                -1.0f, -1.0f, 0.0f,  // 좌하단
                -1.0f, 1.0f, 0.0f,  // 좌상단
                1.0f, -1.0f, 0.0f,  // 우하단
                1.0f, 1.0f, 0.0f   // 우상단
            )

            quadVertices = ByteBuffer.allocateDirect(quadVerticesData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            quadVertices.put(quadVerticesData)
            quadVertices.position(0)

            // 텍스처 좌표 초기화 (ARCore에서 업데이트할 기본값)
            val quadTexCoordsData = floatArrayOf(
                0.0f, 1.0f,  // 좌하단
                0.0f, 0.0f,  // 좌상단
                1.0f, 1.0f,  // 우하단
                1.0f, 0.0f   // 우상단
            )

            quadTexCoords = ByteBuffer.allocateDirect(quadTexCoordsData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            quadTexCoords.put(quadTexCoordsData)
            quadTexCoords.position(0)

            // 3D 모델 셰이더 초기화
            initModelShaders()

            Log.d(TAG, "OpenGL 초기화 완료")
            isGlInitialized = true

        } catch (e: Exception) {
            Log.e(TAG, "OpenGL 초기화 오류: ${e.message}")
            e.printStackTrace()
            isGlInitialized = false
        }
    }

    private fun initModelShaders() {
        // 모델 셰이더 초기화
        val modelVertexShader = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            attribute vec3 a_Normal;
            varying vec3 v_Normal;
            varying vec3 v_ViewPosition;
            
            void main() {
                v_Normal = a_Normal;
                gl_Position = u_MvpMatrix * a_Position;
                v_ViewPosition = (u_MvpMatrix * a_Position).xyz;
            }
        """.trimIndent()

        val modelFragmentShader = """
            precision mediump float;
            uniform vec3 u_LightPos;
            uniform vec3 u_Color;
            varying vec3 v_Normal;
            varying vec3 v_ViewPosition;
            
            void main() {
                vec3 normal = normalize(v_Normal);
                vec3 lightDir = normalize(u_LightPos - v_ViewPosition);
                float diffuse = max(dot(normal, lightDir), 0.2);  // 최소 조명 수준 0.2
                gl_FragColor = vec4(u_Color * diffuse, 1.0);
            }
        """.trimIndent()

        // 셰이더 프로그램 생성
        val vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, modelVertexShader)
        val fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, modelFragmentShader)

        modelShaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(modelShaderProgram, vertexShaderId)
        GLES20.glAttachShader(modelShaderProgram, fragmentShaderId)
        GLES20.glLinkProgram(modelShaderProgram)

        // 프로그램 연결 상태 확인
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(modelShaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(modelShaderProgram)
            GLES20.glDeleteProgram(modelShaderProgram)
            throw RuntimeException("모델 셰이더 프로그램 링크 실패: $log")
        }

        // 셰이더 속성 및 유니폼 위치 가져오기
        modelPositionAttrib = GLES20.glGetAttribLocation(modelShaderProgram, "a_Position")
        modelNormalAttrib = GLES20.glGetAttribLocation(modelShaderProgram, "a_Normal")
        modelMvpMatrixUniform = GLES20.glGetUniformLocation(modelShaderProgram, "u_MvpMatrix")
        modelLightPosUniform = GLES20.glGetUniformLocation(modelShaderProgram, "u_LightPos")
        modelColorUniform = GLES20.glGetUniformLocation(modelShaderProgram, "u_Color")

        Log.d(TAG, "모델 셰이더 초기화 완료")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)

        // ARCore 세션에 화면 크기 알림
        try {
            session.setDisplayGeometry(0, width, height)
            Log.d(TAG, "디스플레이 지오메트리 설정: $width x $height")
        } catch (e: Exception) {
            Log.e(TAG, "디스플레이 지오메트리 설정 오류: ${e.message}")
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (!isGlInitialized) {
            Log.e(TAG, "GL이 초기화되지 않았습니다. 렌더링을 건너뜁니다.")
            return
        }

        try {
            // ARCore 프레임 업데이트
            val frame = session.update()

            // 카메라 배경 렌더링
            drawCameraBackground(frame)

            // 트래킹 상태 로깅 (너무 자주 로깅하면 성능에 영향을 줄 수 있어 낮은 로그 레벨 사용)
            val camera = frame.camera
            Log.v(TAG, "카메라 트래킹 상태: ${camera.trackingState}")

            // 현재 사이트가 있으면 AR 모델 렌더링
            if (camera.trackingState == TrackingState.TRACKING) {
                currentSite?.let {
                    renderARModel(frame, it)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "프레임 렌더링 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun drawCameraBackground(frame: Frame) {
        try {
            // 깊이 테스트 비활성화 (배경은 항상 가장 뒤에)
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)

            // 세션의 카메라 텍스처를 업데이트
            // 주의: session.setCameraTextureName()은 onSurfaceCreated에서 한 번만 호출해야 함
            // 여기서는 이미 설정된 텍스처를 사용

            // 셰이더 프로그램 사용
            GLES20.glUseProgram(cameraProgram)

            // 텍스처 바인딩
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            GLES20.glUniform1i(cameraTextureUniform, 0)

            // 버텍스 및 텍스처 좌표 전달
            GLES20.glVertexAttribPointer(
                cameraPositionAttrib,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                quadVertices
            )
            GLES20.glVertexAttribPointer(
                cameraTexCoordAttrib,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                quadTexCoords
            )

            GLES20.glEnableVertexAttribArray(cameraPositionAttrib)
            GLES20.glEnableVertexAttribArray(cameraTexCoordAttrib)

            // 삼각형 스트립으로 그리기
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            // 정리
            GLES20.glDisableVertexAttribArray(cameraPositionAttrib)
            GLES20.glDisableVertexAttribArray(cameraTexCoordAttrib)

            // 깊이 테스트 다시 활성화
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        } catch (e: Exception) {
            Log.e(TAG, "카메라 배경 렌더링 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun renderARModel(frame: Frame, site: MainActivity.HistoricalSite) {
        try {
            // 모델이 아직 로드되지 않았으면 로드
            if (modelData == null) {
                val modelPath = site.modelPath
                modelData = modelLoader?.loadModelFromAssets(modelPath)
                if (modelData == null) {
                    Log.e(TAG, "모델 로드 실패: $modelPath")
                    return
                }
                Log.d(TAG, "모델 로드 성공: $modelPath")
            }

            // 카메라 포즈 가져오기
            val camera = frame.camera
            val cameraPose = camera.pose

            // 카메라 위치와 방향 가져오기
            // cameraPose.matrix 대신 직접 행렬 얻기
            // 카메라 포즈 행렬 가져오기
            cameraPose.toMatrix(cameraPoseMatrix, 0)

            // 카메라로부터 전방 벡터 계산 (z축 방향)
            val forward = floatArrayOf(0f, 0f, -1f, 0f)
            val forwardTransformed = FloatArray(4)
            Matrix.multiplyMV(forwardTransformed, 0, cameraPoseMatrix, 0, forward, 0)

            // 모델을 카메라 앞 3미터 위치에 배치
            val distanceFromCamera = 3.0f
            val modelPos = floatArrayOf(
                cameraPose.tx() + forwardTransformed[0] * distanceFromCamera,
                cameraPose.ty() + forwardTransformed[1] * distanceFromCamera - 1.0f, // 약간 아래로 배치
                cameraPose.tz() + forwardTransformed[2] * distanceFromCamera
            )

            // 모델 행렬 설정
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, modelPos[0], modelPos[1], modelPos[2])

            // 카메라 방향을 향하도록 회전 (y축 기준)
            val direction = Math.atan2(forwardTransformed[0].toDouble(), forwardTransformed[2].toDouble())
            Matrix.rotateM(modelMatrix, 0, Math.toDegrees(direction).toFloat(), 0f, 1f, 0f)

            // 추가 회전 (모델이 똑바로 서있도록)
            Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)

            // 모델 크기 조정 (OBJ 파일에 따라 적절히 조정)
            val scale = 0.5f
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale)

            // 투영 행렬 및 뷰 행렬 가져오기
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
            camera.getViewMatrix(viewMatrix, 0)

            // MVP 행렬 계산
            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

            // 모델 렌더링 시작
            GLES20.glUseProgram(modelShaderProgram)

            // MVP 행렬 전달
            GLES20.glUniformMatrix4fv(modelMvpMatrixUniform, 1, false, mvpMatrix, 0)

            // 광원 위치 설정 (카메라 위치를 광원으로 사용)
            GLES20.glUniform3f(modelLightPosUniform, cameraPose.tx(), cameraPose.ty(), cameraPose.tz())

            // 모델 색상 설정 (회백색으로 시작)
            GLES20.glUniform3f(modelColorUniform, 0.8f, 0.8f, 0.8f)

            // 버텍스 데이터 바인딩
            GLES20.glEnableVertexAttribArray(modelPositionAttrib)
            GLES20.glVertexAttribPointer(
                modelPositionAttrib, 3, GLES20.GL_FLOAT, false, 0, modelData?.vertexBuffer
            )

            // 법선 데이터 바인딩 (있는 경우에만)
            modelData?.normalBuffer?.let {
                GLES20.glEnableVertexAttribArray(modelNormalAttrib)
                GLES20.glVertexAttribPointer(
                    modelNormalAttrib, 3, GLES20.GL_FLOAT, false, 0, it
                )
            } ?: run {
                // 법선 데이터가 없는 경우, 기본값 사용
                val defaultNormal = floatArrayOf(0f, 1f, 0f)
                GLES20.glVertexAttrib3fv(modelNormalAttrib, defaultNormal, 0)
            }

            // 깊이 테스트 활성화
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            // 컬링 활성화 (선택사항)
            GLES20.glEnable(GLES20.GL_CULL_FACE)
            GLES20.glCullFace(GLES20.GL_BACK)

            // 인덱스 버퍼로 모델 그리기
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                modelData?.numIndices ?: 0,
                GLES20.GL_UNSIGNED_INT,
                modelData?.indexBuffer
            )

            // 정리
            GLES20.glDisableVertexAttribArray(modelPositionAttrib)
            modelData?.normalBuffer?.let {
                GLES20.glDisableVertexAttribArray(modelNormalAttrib)
            }
            GLES20.glDisable(GLES20.GL_CULL_FACE)

        } catch (e: Exception) {
            Log.e(TAG, "모델 렌더링 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    fun setCurrentSite(site: MainActivity.HistoricalSite?) {
        if (site != currentSite) {
            // 다른 사이트로 변경되었으면 모델 데이터 초기화
            modelData = null
            currentSite = site
            Log.d(TAG, "현재 사이트 설정: ${site?.name ?: "없음"}")
        }
    }

    // 셰이더 컴파일 유틸리티 메서드
    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            throw RuntimeException("셰이더 타입 $type 생성 실패")
        }

        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // 컴파일 상태 확인
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("셰이더 컴파일 실패: $log")
        }

        return shader
    }
}