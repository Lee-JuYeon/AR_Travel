package com.jupond.opencv

// 경량 3D 렌더링 관리자
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import org.opencv.core.Mat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ModelRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private val TAG = "ModelRenderer"

    // 모델 데이터
    private var vertexBuffer: FloatBuffer? = null
    private var textureBuffer: FloatBuffer? = null
    private var indexBuffer: ByteBuffer? = null
    private var numIndices = 0

    // 셰이더 프로그램
    private var programId = 0

    // 위치 행렬
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // 텍스처
    private var textureId = 0

    // 깊이 텍스처 관련 - 추가된 부분
    private var depthTextureId = 0
    private var useDepthTexture = false
    private var cameraWidth = 640 // 기본값, 실제 카메라 해상도로 업데이트 필요
    private var cameraHeight = 480 // 기본값, 실제 카메라 해상도로 업데이트 필요

    // LOD 관리
    private var currentLOD = 0
    private var lodModels = arrayOfNulls<Model>(3) // 3단계 LOD

    // 성능 모니터링
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var totalFrameTime = 0L

    // 모델 클래스
    data class Model(
        val vertices: FloatArray,
        val texCoords: FloatArray,
        val indices: ByteArray,
        val numIndices: Int
    )

    private var useCustomModel = false
    private var customModelVertices: FloatBuffer? = null
    private var customModelTexCoords: FloatBuffer? = null
    private var customModelIndices: ByteBuffer? = null
    private var customModelIndexCount = 0

    init {
        // 모델 행렬 초기화
        Matrix.setIdentityM(modelMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // OpenGL ES 설정
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // 셰이더 프로그램 컴파일
        programId = compileShaders()

        // LOD 모델 로드
//        loadLODModels()

        // 텍스처 로드
        textureId = loadTexture()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        // 뷰포트 설정
        GLES20.glViewport(0, 0, width, height)

        // 프로젝션 행렬 계산
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 1000f)

        // 카메라 해상도 업데이트 - 추가된 부분
        cameraWidth = width
        cameraHeight = height
    }

    override fun onDrawFrame(gl: GL10) {
        // 성능 측정 시작
        val startTime = System.nanoTime()

        // 화면 클리어
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 커스텀 모델 또는 LOD 모델 선택
        if (useCustomModel && customModelVertices != null) {
            // 커스텀 모델 사용
            vertexBuffer = customModelVertices
            textureBuffer = customModelTexCoords
            indexBuffer = customModelIndices
            numIndices = customModelIndexCount
        } else {
            // 현재 LOD 모델 선택
            val model = lodModels[currentLOD] ?: return
            setupBuffers(model)
        }

        // 셰이더 사용
        GLES20.glUseProgram(programId)

        // 카메라 위치 (뷰 행렬) 설정
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)

        // MVP 행렬 계산
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // MVP 행렬을 셰이더에 전달
        val mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 텍스처 활성화
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        val textureSamplerHandle = GLES20.glGetUniformLocation(programId, "uTexture")
        GLES20.glUniform1i(textureSamplerHandle, 0)

        // 깊이 텍스처 활성화 (사용 가능한 경우)
        if (useDepthTexture && depthTextureId != 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTextureId)
            val depthSamplerHandle = GLES20.glGetUniformLocation(programId, "uDepthTexture")
            if (depthSamplerHandle != -1) {
                GLES20.glUniform1i(depthSamplerHandle, 1)

                // 깊이 텍스처 사용 여부를 셰이더에 알림
                val useDepthHandle = GLES20.glGetUniformLocation(programId, "uUseDepth")
                if (useDepthHandle != -1) {
                    GLES20.glUniform1i(useDepthHandle, 1)
                }
            }
        }

        // 버텍스 속성 활성화
        val positionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        val texCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        // 모델 그리기
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numIndices, GLES20.GL_UNSIGNED_BYTE, indexBuffer)

        // 버텍스 속성 비활성화
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        // 성능 측정 종료
        val endTime = System.nanoTime()
        val frameDuration = (endTime - startTime) / 1_000_000.0 // 밀리초 단위

        // 성능 통계 업데이트
        frameCount++
        totalFrameTime += frameDuration.toLong()
        if (frameCount % 60 == 0) {
            val avgFrameTime = totalFrameTime.toFloat() / frameCount
            Log.d(TAG, "평균 프레임 렌더링 시간: $avgFrameTime ms (${1000.0f / avgFrameTime} FPS)")
            frameCount = 0
            totalFrameTime = 0
        }
    }

    private fun compileShaders(): Int {
        // 버텍스 셰이더 - 깊이 텍스처 지원 추가
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        // 프래그먼트 셰이더 - 깊이 텍스처 지원 추가
        val fragmentShaderCode = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            uniform sampler2D uDepthTexture;
            uniform int uUseDepth;
            
            void main() {
                vec4 baseColor = texture2D(uTexture, vTexCoord);
                
                if (uUseDepth == 1) {
                    // 깊이 텍스처 사용 시 깊이 정보 적용
                    float depth = texture2D(uDepthTexture, vTexCoord).r;
                    
                    // 깊이에 따른 색상 조정 (예: 거리에 따른 안개 효과)
                    vec4 fogColor = vec4(0.5, 0.5, 0.5, 1.0);
                    float fogFactor = smoothstep(0.0, 1.0, depth);
                    gl_FragColor = mix(baseColor, fogColor, fogFactor * 0.5);
                } else {
                    gl_FragColor = baseColor;
                }
            }
        """.trimIndent()

        // 셰이더 컴파일
        val vertexShaderId = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vertexShaderId, vertexShaderCode)
        GLES20.glCompileShader(vertexShaderId)

        val fragmentShaderId = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fragmentShaderId, fragmentShaderCode)
        GLES20.glCompileShader(fragmentShaderId)

        // 셰이더 프로그램 생성
        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShaderId)
        GLES20.glAttachShader(programId, fragmentShaderId)
        GLES20.glLinkProgram(programId)

        return programId
    }

    private fun loadLODModels() {
        // 간단한 큐브 모델 (예시)
        // 실제 구현에서는 OBJ 파일 등에서 모델 데이터 로드
        // LOD 0 (고품질)
        lodModels[0] = createCubeModel(16) // 세분화된 큐브

        // LOD 1 (중간 품질)
        lodModels[1] = createCubeModel(8)

        // LOD 2 (저품질)
        lodModels[2] = createCubeModel(4) // 단순한 큐브
    }

    private fun createCubeModel(subdivisions: Int): Model {
        // 간단한 큐브 모델 생성 (세분화 수준에 따라)
        // 실제 구현에서는 더 복잡한 모델 생성 로직 필요
        // 이 예제에서는 단순화를 위해 기본 큐브만 생성

        val vertices = floatArrayOf(
            // 전면
            -1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            // 후면
            -1.0f,  1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f
        )

        val texCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,

            1.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        val indices = byteArrayOf(
            // 전면
            0, 1, 2,
            2, 3, 0,

            // 우측면
            1, 5, 6,
            6, 2, 1,

            // 후면
            5, 4, 7,
            7, 6, 5,

            // 좌측면
            4, 0, 3,
            3, 7, 4,

            // 상단면
            4, 5, 1,
            1, 0, 4,

            // 하단면
            3, 2, 6,
            6, 7, 3
        )

        return Model(vertices, texCoords, indices, indices.size)
    }

    private fun setupBuffers(model: Model) {
        // 버텍스 버퍼 설정
        val vbb = ByteBuffer.allocateDirect(model.vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer?.put(model.vertices)
        vertexBuffer?.position(0)

        // 텍스처 좌표 버퍼 설정
        val tbb = ByteBuffer.allocateDirect(model.texCoords.size * 4)
        tbb.order(ByteOrder.nativeOrder())
        textureBuffer = tbb.asFloatBuffer()
        textureBuffer?.put(model.texCoords)
        textureBuffer?.position(0)

        // 인덱스 버퍼 설정
        indexBuffer = ByteBuffer.allocateDirect(model.indices.size)
        indexBuffer?.put(model.indices)
        indexBuffer?.position(0)

        numIndices = model.numIndices
    }

    private fun loadTexture(): Int {
        // 텍스처 생성 (실제 구현에서는 리소스에서 텍스처 로드)
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 텍스처 파라미터 설정
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // 텍스처 데이터 로드 (실제 구현에서는 비트맵 로드)
        // 이 예제에서는 단순 체크무늬 텍스처 생성
        val width = 64
        val height = 64
        val pixels = ByteBuffer.allocateDirect(width * height * 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = if ((x / 8 + y / 8) % 2 == 0) 255.toByte() else 0.toByte()
                pixels.put((y * width + x) * 4, value) // R
                pixels.put((y * width + x) * 4 + 1, value) // G
                pixels.put((y * width + x) * 4 + 2, value) // B
                pixels.put((y * width + x) * 4 + 3, 255.toByte()) // A
            }
        }
        pixels.position(0)

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        return textureId
    }

    // 모델 위치 업데이트 (호모그래피 행렬 기반)
    fun updateModelMatrix(homographyMat: Mat?) {
        if (homographyMat == null) return

        // 호모그래피 행렬을 OpenGL 변환 행렬로 변환
        val transformData = FloatArray(16)

        // 호모그래피를 3x3 행렬로 처리
        val h = DoubleArray(9)
        homographyMat.get(0, 0, h)

        // 간소화된 변환 방식 (실제로는 더 정교한 변환 필요)
        Matrix.setIdentityM(modelMatrix, 0)

        // 스케일 및 회전 적용
        modelMatrix[0] = h[0].toFloat()
        modelMatrix[1] = h[3].toFloat()
        modelMatrix[2] = 0f

        modelMatrix[4] = h[1].toFloat()
        modelMatrix[5] = h[4].toFloat()
        modelMatrix[6] = 0f

        // 평행 이동 적용
        modelMatrix[12] = h[2].toFloat() * 0.01f // 스케일 조정
        modelMatrix[13] = h[5].toFloat() * 0.01f
        modelMatrix[14] = -5f // Z 위치 조정
    }

    // GPS 기반 모델 위치 업데이트 - 추가된 메소드
    fun updateModelMatrixFromGPS(matrix: FloatArray) {
        // 모델 행렬 복사
        System.arraycopy(matrix, 0, modelMatrix, 0, 16)
    }

    // 깊이 텍스처 업데이트 - 추가된 메소드
    fun updateDepthTexture(depthTextureData: FloatBuffer) {
        // 깊이 텍스처 ID가 없으면 생성
        if (depthTextureId == 0) {
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            depthTextureId = textureIds[0]
        }

        // 깊이 텍스처 업데이트
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTextureId)

        // 텍스처 파라미터 설정
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // 텍스처 데이터 업로드
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
            cameraWidth, cameraHeight, 0,
            GLES20.GL_LUMINANCE, GLES20.GL_FLOAT, depthTextureData
        )

        // 깊이 텍스처 활성화 플래그 설정
        useDepthTexture = true
    }

    // LOD 레벨 설정 (기기 성능에 따라 자동 조정)
    fun setLODLevel(level: Int) {
        currentLOD = level.coerceIn(0, lodModels.size - 1)
    }

    // 성능에 따른 LOD 자동 조정
    fun adjustLODBasedOnPerformance(averageFrameTime: Float) {
        currentLOD = when {
            averageFrameTime < 16 -> 0 // 60fps 이상 -> 고품질
            averageFrameTime < 33 -> 1 // 30-60fps -> 중간 품질
            else -> 2 // 30fps 미만 -> 저품질
        }
    }

    fun setModelData(vertexBuffer: FloatBuffer, texCoordBuffer: FloatBuffer, indexBuffer: ShortBuffer, indexCount: Int) {
        // ShortBuffer에서 ByteBuffer로 변환
        val byteIndexBuffer = ByteBuffer.allocateDirect(indexCount * 2)
        byteIndexBuffer.order(ByteOrder.nativeOrder())

        for (i in 0 until indexCount) {
            val value = indexBuffer.get(i)
            byteIndexBuffer.putShort(value)
        }
        byteIndexBuffer.position(0)

        // 커스텀 모델 데이터 설정
        customModelVertices = vertexBuffer
        customModelTexCoords = texCoordBuffer
        customModelIndices = byteIndexBuffer
        customModelIndexCount = indexCount
        useCustomModel = true
    }
}