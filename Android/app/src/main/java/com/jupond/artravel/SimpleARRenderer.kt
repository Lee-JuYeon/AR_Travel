package com.jupond.artravel

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.Session
import java.io.IOException
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

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated() 호출됨")
        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)  // 배경색을 진한 회색으로 설정

        try {
            // 카메라 텍스처 생성
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            cameraTextureId = textures[0]

            Log.d(TAG, "카메라 텍스처 ID: $cameraTextureId")

            // ARCore에 텍스처 ID 전달
            session.setCameraTextureName(cameraTextureId)

            // 카메라 텍스처 설정
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            // 카메라 셰이더 초기화
            val vertexShader = """
                attribute vec4 a_Position;
                attribute vec2 a_TexCoord;
                varying vec2 v_TexCoord;
                void main() {
                    gl_Position = a_Position;
                    v_TexCoord = a_TexCoord;
                }
            """

            val fragmentShader = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES u_Texture;
                varying vec2 v_TexCoord;
                void main() {
                    gl_FragColor = texture2D(u_Texture, v_TexCoord);
                }
            """

            // 셰이더 프로그램 생성
            val vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
            val fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

            cameraProgram = GLES20.glCreateProgram()
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
                -1.0f,  1.0f, 0.0f,  // 좌상단
                1.0f, -1.0f, 0.0f,  // 우하단
                1.0f,  1.0f, 0.0f   // 우상단
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

            Log.d(TAG, "OpenGL 초기화 완료")
            isGlInitialized = true

        } catch (e: Exception) {
            Log.e(TAG, "OpenGL 초기화 오류: ${e.message}")
            e.printStackTrace()
            isGlInitialized = false
        }
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
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)  // 회색으로 변경

        if (!isGlInitialized) {
            Log.e(TAG, "GL이 초기화되지 않았습니다. 렌더링을 건너뜁니다.")
            return
        }

        try {
            // ARCore 프레임 업데이트
            val frame = session.update()

            // 카메라 배경 렌더링
            drawCameraBackground(frame)

            // 트래킹 상태 로깅
            val camera = frame.camera
            Log.d(TAG, "카메라 트래킹 상태: ${camera.trackingState}")

            // 현재 사이트가 있으면 AR 모델 렌더링
            currentSite?.let {
                renderARModel(frame, it)
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

            // 새로운 방식: 세션의 카메라 텍스처를 업데이트
            session.setCameraTextureName(cameraTextureId)

            // 셰이더 프로그램 사용
            GLES20.glUseProgram(cameraProgram)

            // 텍스처 바인딩
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            GLES20.glUniform1i(cameraTextureUniform, 0)

            // 버텍스 및 텍스처 좌표 전달
            GLES20.glVertexAttribPointer(cameraPositionAttrib, 3, GLES20.GL_FLOAT, false, 0, quadVertices)
            GLES20.glVertexAttribPointer(cameraTexCoordAttrib, 2, GLES20.GL_FLOAT, false, 0, quadTexCoords)

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
        // 모델 렌더링 로직 (이 부분은 나중에 구현)
        Log.v(TAG, "모델 렌더링: ${site.name}")
    }

    fun setCurrentSite(site: MainActivity.HistoricalSite?) {
        currentSite = site
        Log.d(TAG, "현재 사이트 설정: ${site?.name ?: "없음"}")
    }

    // 셰이더 컴파일 유틸리티 메서드
    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
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