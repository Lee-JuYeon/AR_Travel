package com.jupond.opencv


import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.opencv.calib3d.StereoSGBM
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.photo.Photo

// 깊이 맵 향상을 위한 코드
class EnhancedDepthEstimationSystem {
    private val TAG = "EnhancedDepthEstimation"

    // 스테레오 매칭 객체
    private var stereoMatcher: StereoSGBM? = null

    // 카메라 이동으로 스테레오뷰 시뮬레이션을 위한 변수
    private var prevFrame: Mat? = null
    private var currentFrame: Mat? = null

    // 깊이 맵
    private var depthMap: Mat? = null
    private var depthMapUpdated = false

    // 카메라 이동 추적
    private val cameraTranslation = MatOfFloat(0f, 0f, 0f)

    init {
        // 스테레오 매칭 초기화
        initStereoMatcher()
    }

    private fun initStereoMatcher() {
        // SGBM 매칭 파라미터 설정
        stereoMatcher = StereoSGBM.create(
            0,     // minDisparity
            128,   // numDisparities
            3,     // blockSize
            8 * 3 * 3 * 3,  // P1
            32 * 3 * 3 * 3,  // P2
            1,     // disp12MaxDiff
            0,     // preFilterCap
            10,    // uniquenessRatio
            20,    // speckleWindowSize
            1,     // speckleRange
            StereoSGBM.MODE_SGBM  // mode
        )
    }

    // 향상된 깊이 추정
    fun estimateDepth(frame: Mat): Mat {
        // 그레이스케일 변환
        val gray = Mat()
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY)

        // 첫 프레임 처리
        if (prevFrame == null) {
            prevFrame = gray.clone()
            depthMap = Mat(gray.size(), CvType.CV_32F, Scalar(0.0))
            return depthMap!!
        }

        // 현재 프레임 저장
        currentFrame = gray.clone()

        // 카메라 이동 추정
        estimateCameraMotion()

        // 이동이 충분하면 깊이 계산
        val translation = cameraTranslation.toArray()
        val movement = Math.sqrt(translation[0].toDouble() * translation[0].toDouble() +
                translation[1].toDouble() * translation[1].toDouble())

        if (movement > 2.0) {  // 임계값: 충분한 이동
            // 스테레오 정류화 (카메라 이동에 기반한 가상 스테레오)
            val rectified = rectifyFrames()

            if (rectified != null) {
                // 시차 맵 계산
                val disparity = Mat()
                stereoMatcher?.compute(rectified.first, rectified.second, disparity)

                // 시차 맵 정규화
                val normalizedDisparity = Mat()
                Core.normalize(disparity, normalizedDisparity, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

                // 깊이 맵으로 변환
                convertDisparityToDepth(normalizedDisparity)

                // 깊이 맵 개선
                enhanceDepthMap()

                depthMapUpdated = true

                // 다음 계산을 위해 현재 프레임을 이전 프레임으로 설정
                prevFrame?.release()
                prevFrame = currentFrame?.clone()
                cameraTranslation.fromArray(0f, 0f, 0f)
            }
        }

        return depthMap!!
    }

    // 카메라 이동 추정
    private fun estimateCameraMotion() {
        // 특징점 검출
        val detector = ORB.create()
        val prevKps = MatOfKeyPoint()
        val currKps = MatOfKeyPoint()
        val prevDesc = Mat()
        val currDesc = Mat()

        detector.detectAndCompute(prevFrame, Mat(), prevKps, prevDesc)
        detector.detectAndCompute(currentFrame, Mat(), currKps, currDesc)

        // 특징점 매칭
        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
        val matches = ArrayList<MatOfDMatch>()
        matcher.knnMatch(prevDesc, currDesc, matches, 2)

        // 좋은 매칭점 선택
        val goodMatches = ArrayList<DMatch>()
        for (match in matches) {
            val matchArray = match.toArray()
            if (matchArray.size >= 2) {
                if (matchArray[0].distance < 0.7 * matchArray[1].distance) {
                    goodMatches.add(matchArray[0])
                }
            }
        }

        if (goodMatches.size >= 8) {
            // 매칭점을 이용하여 E 행렬 계산
            val prevPts = ArrayList<Point>()
            val currPts = ArrayList<Point>()

            for (m in goodMatches) {
                prevPts.add(prevKps.toArray()[m.queryIdx].pt)
                currPts.add(currKps.toArray()[m.trainIdx].pt)
            }

            val prevPtsMat = MatOfPoint2f()
            prevPtsMat.fromList(prevPts)

            val currPtsMat = MatOfPoint2f()
            currPtsMat.fromList(currPts)

            // 기본 카메라 내부 파라미터 (추정값)
            val cameraMatrix = Mat.eye(3, 3, CvType.CV_64F)
            cameraMatrix.put(0, 0, 1000.0)  // 초점 거리 x
            cameraMatrix.put(1, 1, 1000.0)  // 초점 거리 y
            cameraMatrix.put(0, 2, prevFrame!!.cols() / 2.0)  // 주점 x
            cameraMatrix.put(1, 2, prevFrame!!.rows() / 2.0)  // 주점 y

            // 본질 행렬 계산
            val E = Calib3d.findEssentialMat(
                prevPtsMat, currPtsMat, cameraMatrix,
                Calib3d.RANSAC, 0.999, 1.0
            )

            // R, t 분해
            val R = Mat()
            val t = Mat()
            Calib3d.recoverPose(E, prevPtsMat, currPtsMat, cameraMatrix, R, t)

            // 이동 벡터 업데이트
            val tx = t.get(0, 0)[0].toFloat()
            val ty = t.get(1, 0)[0].toFloat()
            val tz = t.get(2, 0)[0].toFloat()

            cameraTranslation.fromArray(tx, ty, tz)
        }
    }

    // 카메라 이동에 기반한 가상 스테레오 정류화
    private fun rectifyFrames(): Pair<Mat, Mat>? {
        // 이동 방향에 따른 영상 변환
        val translation = cameraTranslation.toArray()

        // 주로 수평 이동이 있는 경우에만 처리
        if (Math.abs(translation[0]) > Math.abs(translation[1]) * 1.5) {
            // 수평 방향 이동 - 스테레오뷰로 사용 가능
            val warpMat = Mat.eye(2, 3, CvType.CV_32F)

            // 이동 방향에 따라 와핑 방향 결정
            val direction = if (translation[0] > 0) 1 else -1

            // 첫 번째 이미지 (기준 이미지)
            val rect1 = Mat()
            prevFrame?.copyTo(rect1)

            // 두 번째 이미지 (수평 이동)
            val rect2 = Mat()
            warpMat.put(0, 2, direction * 20.0)  // 수평 이동
            Imgproc.warpAffine(currentFrame, rect2, warpMat, currentFrame!!.size())

            return Pair(rect1, rect2)
        }

        return null
    }

    // 시차 맵을 깊이 맵으로 변환
    private fun convertDisparityToDepth(disparity: Mat) {
        // 간단한 변환: 시차가 클수록 깊이는 작음
        Core.convertScaleAbs(disparity, depthMap, -1.0, 255.0)
        depthMap?.convertTo(depthMap, CvType.CV_32F)

        // 역수 관계 적용 (시차 -> 깊이)
        for (y in 0 until depthMap!!.rows()) {
            for (x in 0 until depthMap!!.cols()) {
                val d = depthMap!!.get(y, x)[0]
                if (d > 0) {
                    // 정규화된 깊이 값 (0~1)
                    val normalizedDepth = 1.0 - (d / 255.0)
                    depthMap!!.put(y, x, normalizedDepth)
                }
            }
        }
    }

    // 깊이 맵 개선
    private fun enhanceDepthMap() {
        // 깊이 맵에 잡음 제거 필터 적용
        val tempDepth = Mat()
        Photo.fastNlMeansDenoising(depthMap, tempDepth)

        // 양방향 필터로 에지 보존 스무딩
        Imgproc.bilateralFilter(tempDepth, depthMap!!, 9, 75.0, 75.0)

        // 홀 채우기 (인페인팅)
        val mask = Mat(depthMap!!.size(), CvType.CV_8U)
        Core.compare(depthMap!!, Scalar(0.01), mask, Core.CMP_LT)
        Photo.inpaint(depthMap!!, mask, depthMap!!, 3.0, Photo.INPAINT_NS)

        tempDepth.release()
        mask.release()
    }

    // 깊이 맵을 OpenGL 깊이 텍스처로 변환
    fun getDepthTextureData(): FloatBuffer {
        if (depthMap == null) {
            return FloatBuffer.allocate(4)
        }

        val width = depthMap!!.cols()
        val height = depthMap!!.rows()

        val buffer = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val depth = depthMap!!.get(y, x)[0]
                val normalizedDepth = Math.min(1.0f, Math.max(0.0f, depth.toFloat()))
                buffer.put(y * width + x, normalizedDepth)
            }
        }

        buffer.position(0)
        return buffer
    }

    // 깊이 맵이 업데이트되었는지 확인
    fun isDepthMapUpdated(): Boolean {
        val updated = depthMapUpdated
        depthMapUpdated = false
        return updated
    }

    // 리소스 해제
    fun release() {
        prevFrame?.release()
        currentFrame?.release()
        depthMap?.release()
    }
}