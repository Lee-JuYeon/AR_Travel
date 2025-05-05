package com.jupond.opencv

import org.opencv.core.*
import org.opencv.features2d.ORB
import org.opencv.calib3d.Calib3d
import org.opencv.imgproc.Imgproc
import android.util.Log
import org.opencv.features2d.DescriptorMatcher

// 특징점 감지 및 트래킹 클래스
class FeatureTracker {
    private val TAG = "FeatureTracker"
    private val detector = ORB.create(500) // 최대 500개 특징점 감지
    private val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)

    private var prevKeypoints: MatOfKeyPoint? = null
    private var prevDescriptors: Mat? = null
    private var lastHomography: Mat? = null

    // 이미지에서 특징점 감지
    fun detectFeatures(image: Mat): MatOfKeyPoint {
        // 그레이스케일 변환
        val gray = Mat()
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY)

        // 특징점 감지
        val keypoints = MatOfKeyPoint()
        detector.detect(gray, keypoints)

        Log.d(TAG, "감지된 특징점 수: ${keypoints.toArray().size}")
        return keypoints
    }

    // 특징점 매칭 및 호모그래피 계산
    fun trackFeatures(image: Mat): Mat? {
        // 현재 프레임에서 특징점 및 디스크립터 계산
        val gray = Mat()
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY)

        val keypoints = MatOfKeyPoint()
        val descriptors = Mat()
        detector.detectAndCompute(gray, Mat(), keypoints, descriptors)

        // 이전 특징점이 없으면 현재 특징점 저장 후 종료
        if (prevKeypoints == null || prevDescriptors == null || prevDescriptors!!.empty()) {
            prevKeypoints = keypoints
            prevDescriptors = descriptors
            return null
        }

        // 특징점 매칭
        val matches = ArrayList<MatOfDMatch>()
        matcher.knnMatch(descriptors, prevDescriptors, matches, 2)

        // 좋은 매칭점만 선택 (Lowe's ratio test)
        val goodMatches = ArrayList<DMatch>()
        for (match in matches) {
            val matchArray = match.toArray()
            if (matchArray.size >= 2) {
                if (matchArray[0].distance < 0.7 * matchArray[1].distance) {
                    goodMatches.add(matchArray[0])
                }
            }
        }

        Log.d(TAG, "좋은 매칭점 수: ${goodMatches.size}")

        // 매칭점이 충분하면 호모그래피 계산
        if (goodMatches.size >= 10) {
            val prevPts = ArrayList<Point>()
            val currPts = ArrayList<Point>()

            for (m in goodMatches) {
                prevPts.add(prevKeypoints!!.toArray()[m.trainIdx].pt)
                currPts.add(keypoints.toArray()[m.queryIdx].pt)
            }

            val prevPtsMat = MatOfPoint2f()
            prevPtsMat.fromList(prevPts)

            val currPtsMat = MatOfPoint2f()
            currPtsMat.fromList(currPts)

            // RANSAC을 사용한 호모그래피 계산
            val homography = Calib3d.findHomography(prevPtsMat, currPtsMat, Calib3d.RANSAC, 5.0)

            if (!homography.empty()) {
                lastHomography = homography
                prevKeypoints = keypoints
                prevDescriptors = descriptors
                return homography
            }
        }

        prevKeypoints = keypoints
        prevDescriptors = descriptors
        return lastHomography
    }

    // 호모그래피를 사용하여 이미지에 건물 모델 오버레이 표시
    fun drawModelOverlay(frame: Mat, homography: Mat?, corners: MatOfPoint2f): Mat {
        if (homography == null || homography.empty()) {
            return frame
        }

        // 모델의 모서리 변환
        val transformedCorners = MatOfPoint2f()
        Core.perspectiveTransform(corners, transformedCorners, homography)

        // 변환된 모서리를 이용하여 모델 윤곽 그리기
        val result = frame.clone()
        val cornersArray = transformedCorners.toArray()

        if (cornersArray.size >= 4) {
            // 모델 윤곽선 그리기
            Imgproc.line(result, cornersArray[0], cornersArray[1], Scalar(0.0, 255.0, 0.0), 4)
            Imgproc.line(result, cornersArray[1], cornersArray[2], Scalar(0.0, 255.0, 0.0), 4)
            Imgproc.line(result, cornersArray[2], cornersArray[3], Scalar(0.0, 255.0, 0.0), 4)
            Imgproc.line(result, cornersArray[3], cornersArray[0], Scalar(0.0, 255.0, 0.0), 4)
        }

        return result
    }

    // 재설정
    fun reset() {
        prevKeypoints = null
        prevDescriptors = null
        lastHomography = null
    }
}