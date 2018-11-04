package com.foxconn.qrscan;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import java.util.ArrayList;
import java.util.List;

public class OpenCVFrameRender extends FrameRender {
    private MatToQRCallBack mCallBack;

    public OpenCVFrameRender(CameraCalibrator calibrator, MatToQRCallBack callBack) {
        mCalibrator = calibrator;
        mCallBack = callBack;
    }

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Mat renderedFrame = inputFrame.rgba();
        Mat renderedFrame = new Mat(inputFrame.rgba().size(), inputFrame.rgba().type());
//        //畸变矫正
        Imgproc.undistort(inputFrame.rgba(), renderedFrame,
                mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients());
        //二值化灰度
        Mat srcColor = new Mat();
        Imgproc.cvtColor(renderedFrame, srcColor, Imgproc.COLOR_RGB2GRAY);
        //二值化轮廓
        Mat srcGrayResizeThresh = new Mat();
        Imgproc.adaptiveThreshold(srcColor, srcGrayResizeThresh, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 33, 0);
        srcColor.release();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(srcGrayResizeThresh, contours, hierarchy,
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        //轮廓绘制
        int ic = 0;
        int parentIdx = -1;
        List<MatOfPoint> contours2 = new ArrayList<MatOfPoint>();
        for (int i = 0; i < contours.size(); i++) {
            if (hierarchy.get(0, i)[2] != -1 && ic == 0) {
                parentIdx = i;
                ic++;
            } else if (hierarchy.get(0, i)[2] != -1) {
                ic++;
            } else if (hierarchy.get(0, i)[2] == -1) {
                ic = 0;
                parentIdx = -1;
            }

            if (ic >= 2) {
                contours2.add(contours.get(parentIdx));
                ic = 0;
                parentIdx = -1;
            }
        }
        for (int i = 0; i < contours2.size(); i++) {
            MatOfPoint2f NewMtx = new MatOfPoint2f(contours2.get(i).toArray());
            RotatedRect rotRect = Imgproc.minAreaRect(NewMtx);
            Rect rect = rotRect.boundingRect();
            Point vertices[] = new Point[4];
            rotRect.points(vertices);
            List<Point> rectArea = new ArrayList<Point>();
            for (int n = 0; n < 4; n++) {
                Point temp = new Point();
                temp.x = vertices[n].x;
                temp.y = vertices[n].y;
                rectArea.add(temp);
            }
            Mat rectMat = Converters.vector_Point_to_Mat(rectArea);
            double minRectArea = Imgproc.contourArea(rectMat);
            Point center = new Point();
            float radius[] = {0};
            Imgproc.minEnclosingCircle(NewMtx, center, radius);
            if (minRectArea < 300 ||
                    minRectArea > 6000
                    || minRectArea < radius[0] * radius[0] * 1.57) {
                contours2.remove(i);
                continue;
            }
            if (rect.width / rect.height > 0.8 && rect.width / rect.height < 1.2) {
                contours2.remove(i);
            }
        }
        if (contours2.size() >= 2) {
            Imgproc.drawContours(renderedFrame, contours2, -1, new Scalar(255, 0, 0));
            if (mCallBack != null) {
                mCallBack.MatToQR(srcGrayResizeThresh);
            }
        }
//        srcGrayResizeThresh.release();
        hierarchy.release();
        return srcGrayResizeThresh;
    }
}