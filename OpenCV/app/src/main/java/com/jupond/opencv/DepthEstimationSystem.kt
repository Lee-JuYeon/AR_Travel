package com.jupond.opencv

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.opencv.video.Video


//Depth 추정 및 통합 시스템

class DepthEstimationSystem {
    private val TAG = "DepthEstimationSystem"

    // 이전 프레임 정보
    private var prevGray: Mat? = null
    private var prevKeypoints = ArrayList<Point>()

    // 시차 계산을 위한 옵티컬 플로우 트래커
    private val maxCorners = 500
    private val qualityLevel = 0.01
    private val minDistance = 10.0

    // 깊이 맵
    private var depthMap: Mat? = null
    private var depthMapUpdated = false

    // 깊이 맵 생성
    fun estimateDepth(frame: Mat): Mat {
        // 현재 프레임을 그레이스케일로 변환
        val gray = Mat()
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY)

        if (prevGray == null) {
            // 첫 프레임인 경우 초기화
            prevGray = gray.clone()

            // 특징점 검출
            val corners = MatOfPoint()
            Imgproc.goodFeaturesToTrack(
                gray, corners, maxCorners,
                qualityLevel, minDistance
            )

            prevKeypoints = ArrayList(corners.toList())

            // 초기 깊이 맵 생성 (모두 최대 깊이로 설정)
            depthMap = Mat(gray.size(), CvType.CV_32F, Scalar(100.0))

            return depthMap!!
        }

        // 이전 특징점을 현재 프레임에서 트래킹
        if (prevKeypoints.isNotEmpty()) {
            val prevPts = MatOfPoint2f()
            prevPts.fromList(prevKeypoints)

            val nextPts = MatOfPoint2f()
            val status = MatOfByte()
            val err = MatOfFloat()

            // 옵티컬 플로우 계산
            val criteria = TermCriteria(
                TermCriteria.COUNT + TermCriteria.EPS,
                10, 0.03
            )

            Video.calcOpticalFlowPyrLK(
                prevGray, gray, prevPts, nextPts,
                status, err, Size(15.0, 15.0), 2, criteria
            )

            // 트래킹 결과에서 깊이 업데이트
            val nextPtsList = nextPts.toList()
            val statusArr = status.toArray()

            if (statusArr.isNotEmpty() && nextPtsList.isNotEmpty()) {
                updateDepthMap(prevKeypoints, nextPtsList, statusArr, gray.size())
                depthMapUpdated = true
            }

            // 새로운 특징점 검출
            val corners = MatOfPoint()
            Imgproc.goodFeaturesToTrack(
                gray, corners, maxCorners,
                qualityLevel, minDistance
            )

            prevKeypoints = ArrayList(corners.toList())
        }

        // 현재 프레임을 이전 프레임으로 저장
        prevGray?.release()
        prevGray = gray.clone()

        return depthMap!!
    }

    // 옵티컬 플로우 결과로 깊이 맵 업데이트
    private fun updateDepthMap(
        prevPoints: List<Point>,
        currentPoints: List<Point>,
        status: ByteArray,
        size: Size
    ) {
        // 깊이 맵이 없으면 생성
        if (depthMap == null) {
            depthMap = Mat(size, CvType.CV_32F, Scalar(100.0))
        }

        // 두 프레임 간의 특징점 이동을 기반으로 깊이 추정
        for (i in prevPoints.indices) {
            if (i >= status.size || status[i] == 0.toByte() || i >= currentPoints.size) continue

            val prev = prevPoints[i]
            val curr = currentPoints[i]

            // 이동 거리 계산 (시차)
            val dx = prev.x - curr.x
            val dy = prev.y - curr.y
            val disparity = Math.sqrt(dx * dx + dy * dy)

            // 시차를 깊이로 변환 (간단한 역비례 관계)
            val depth = if (disparity > 0) 100.0 / disparity else 100.0
            val clampedDepth = Math.min(100.0, Math.max(1.0, depth))

            // 깊이 맵 업데이트 (특징점 주변 영역)
            val radius = 10
            val roi = Rect(
                Math.max(0, curr.x.toInt() - radius),
                Math.max(0, curr.y.toInt() - radius),
                Math.min(2 * radius, depthMap!!.cols() - curr.x.toInt() + radius),
                Math.min(2 * radius, depthMap!!.rows() - curr.y.toInt() + radius)
            )

            if (roi.width > 0 && roi.height > 0) {
                val roiMat = depthMap!!.submat(roi)
                roiMat.setTo(Scalar(clampedDepth))
                roiMat.release()
            }
        }

        // 깊이 맵 스무딩
        Imgproc.GaussianBlur(depthMap, depthMap, Size(15.0, 15.0), 2.0)
    }

    // 깊이 맵을 텍스처로 변환
    fun getDepthMapTexture(): ByteBuffer {
        if (depthMap == null) {
            return ByteBuffer.allocateDirect(4)
        }

        val width = depthMap!!.cols()
        val height = depthMap!!.rows()

        val buffer = ByteBuffer.allocateDirect(width * height * 4)
            .order(ByteOrder.nativeOrder())

        // 깊이 값을 RGBA로 변환 (시각화 목적)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val depth = depthMap!!.get(y, x)[0]
                val normalizedDepth = Math.min(1.0, Math.max(0.0, depth / 100.0))

                // 깊이를 색상으로 변환 (가까울수록 빨간색, 멀수록 파란색)
                val r = (255 * (1.0 - normalizedDepth)).toInt()
                val g = 0
                val b = (255 * normalizedDepth).toInt()
                val a = 255

                val idx = (y * width + x) * 4
                buffer.put(idx, r.toByte())
                buffer.put(idx + 1, g.toByte())
                buffer.put(idx + 2, b.toByte())
                buffer.put(idx + 3, a.toByte())
            }
        }

        buffer.position(0)
        return buffer
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
                val normalizedDepth = Math.min(1.0f, Math.max(0.0f, (depth / 100.0).toFloat()))
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
        prevGray?.release()
        depthMap?.release()
    }

    private var downsampleFactor = 1 // 기본값은 1 (원본 해상도)

    // 깊이 맵 연산 최적화 (저사양 기기용)
    fun optimizeForLowEndDevice(isLowEnd: Boolean) {
        if (isLowEnd) {
            // 저해상도로 처리 후 업스케일
            downsampleFactor = 2 // 원본 이미지 크기의 1/2로 처리
        } else {
            downsampleFactor = 1 // 원본 해상도로 처리
        }
    }

    // 이미지 다운샘플링
    private fun downsampleImage(image: Mat): Mat {
        if (downsampleFactor <= 1) return image.clone()

        val downsampled = Mat()
        Imgproc.resize(
            image,
            downsampled,
            Size(image.cols() / downsampleFactor.toDouble(), image.rows() / downsampleFactor.toDouble())
        )
        return downsampled
    }

    // 이미지 업샘플링
    private fun upsampleImage(image: Mat, originalSize: Size): Mat {
        if (downsampleFactor <= 1) return image.clone()

        val upsampled = Mat()
        Imgproc.resize(image, upsampled, originalSize)
        return upsampled
    }
}