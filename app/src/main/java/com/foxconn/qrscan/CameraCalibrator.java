package com.foxconn.qrscan;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

public class CameraCalibrator {
    private static final String TAG = "CameraCalibrator";

    private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients = new Mat();
    private int mFlags;
    private double mRms;
    private double mSquareSize = 0.0181;//0.0181
    private Size mImageSize;

    public CameraCalibrator(int width, int height) {
        mImageSize = new Size(width, height);
        mFlags = Calib3d.CALIB_FIX_PRINCIPAL_POINT +
                 Calib3d.CALIB_ZERO_TANGENT_DIST +
                 Calib3d.CALIB_FIX_ASPECT_RATIO +
                 Calib3d.CALIB_FIX_K4 +
                 Calib3d.CALIB_FIX_K5;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
    }
    public Mat getCameraMatrix() {
        return mCameraMatrix;
    }

    public Mat getDistortionCoefficients() {
        return mDistortionCoefficients;
    }

}
