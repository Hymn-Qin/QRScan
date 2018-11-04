package com.foxconn.qrscan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = MainActivity.class.getSimpleName();


    public final static String ACTION_SEND_QRWIFI = "com.foxconn.qrwifi";
    private CameraBridgeViewBase mOpenCvCameraView;
    private CameraCalibrator mCalibrator;
    private int mWidth;
    private int mHeight;
    boolean start = true;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    static final Set<BarcodeFormat> QR_CODE_FORMATS = EnumSet.of(BarcodeFormat.QR_CODE);
    static final Set<BarcodeFormat> DATA_MATRIX_FORMATS = EnumSet.of(BarcodeFormat.DATA_MATRIX);
    static final Set<BarcodeFormat> AZTEC_FORMATS = EnumSet.of(BarcodeFormat.AZTEC);
    static final Set<BarcodeFormat> PDF417_FORMATS = EnumSet.of(BarcodeFormat.PDF_417);
    public static final Set<BarcodeFormat> PRODUCT_FORMATS = EnumSet.of(BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED);

    private static final Set<BarcodeFormat> ONE_D_FORMATS = EnumSet.copyOf(PRODUCT_FORMATS);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = findViewById(R.id.camera_calibration_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.SetCaptureFormat(2);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(myBroadcastReceiver);
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /*
    注册广播
     */
    public void registerCloseSelfBroadCast(BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.foxconn.close.zxing");
        this.registerReceiver(receiver, filter);
    }

    public void sendQRWiFiBroadCast(String result) {
        Log.d(TAG, "result = " + result);
        Intent i = new Intent(ACTION_SEND_QRWIFI);

        i.putExtra("msg", result);
        this.sendBroadcast(i);
    }

    public void openSmartCamera() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName cn = new ComponentName("com.zzdc.abb.smartcamera", "com.zzdc.abb.smartcamera.controller.MainActivity");
        i.setComponent(cn);
        startActivity(i);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            Log.d(TAG, "this camera" + width + " * " +height);
            mCalibrator = new CameraCalibrator(mWidth, mHeight);
            double[] cameraMatrixArray = new double[]{6979.00760238609, 0, 512, 0, 6979.00760238609, 384, 0, 0, 1};
            double[] distortionCoefficientsArray = new double[]{-2.461133905207975, -1775.194709270299, 0, 0, 0};
            mCalibrator.getCameraMatrix().put(0, 0, cameraMatrixArray);
            mCalibrator.getDistortionCoefficients().put(0, 0, distortionCoefficientsArray);

            final MultiFormatReader formatReader = new MultiFormatReader();
            final Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(
                    2);
            // 可以解析的编码类型
            Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>();
            if (decodeFormats.isEmpty()) {
                decodeFormats = new Vector<BarcodeFormat>();

                // 这里设置可扫描的类型，我这里选择了都支持
                decodeFormats.addAll(ONE_D_FORMATS);
                decodeFormats.addAll(QR_CODE_FORMATS);
                decodeFormats.addAll(DATA_MATRIX_FORMATS);
            }
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF8");//"UTF8"
            mOnCameraFrameRender = new OpenCVFrameRender(mCalibrator, new MatToQRCallBack() {
                @Override
                public void MatToQR(Mat renderedFrame) {
                    try {
                        Log.d(TAG, "检测到有二维码,开始解析");
                        Bitmap bmpCanny = Bitmap.createBitmap(renderedFrame.cols(), renderedFrame.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(renderedFrame, bmpCanny);
                        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                                new BitmapLuminanceSource(bmpCanny)
                        ));
                        formatReader.setHints(hints);
                        Result result = formatReader.decodeWithState(binaryBitmap);
                        if (result.getText() != null && result.getText().contains(",\"q\":") && start) {
                            start = false;
                            Log.d(TAG, "读取到的二维码" + result.toString());
//                            openSmartCamera();
                            sendQRWiFiBroadCast(result.getText());
//                            finish();

                        } else {
                            start = true;
                            sendQRWiFiBroadCast("ERROR");
                            //对此次扫描结果不满意可以调用
                            Log.d(TAG, "该二维码非法" + result.toString());
                        }
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        formatReader.reset();
                    }
                }
            });
        }
    }

    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "openCVCamera stop");
    }

    OpenCVFrameRender mOnCameraFrameRender;

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return mOnCameraFrameRender.render(inputFrame);
    }

}
