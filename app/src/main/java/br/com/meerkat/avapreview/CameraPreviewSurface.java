package br.com.meerkat.avapreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.res.Configuration;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import br.com.meerkat.ava.Ava;

/**
 * Created by meerkat on 4/29/16.
 */
public class CameraPreviewSurface extends SurfaceView implements SurfaceHolder.Callback{
    private Ava.CameraType camType = Ava.CameraType.FRONT_CAMERA;
    private int cameraWidth = 720;
    private int cameraHeight = 480;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private CameraDetectorCaller mCamDetector = new CameraDetectorCaller();
    public static final String TAG = "CameraPreviewSurface";
    public SurfaceOverlay overlay;
    private TextView textView;
    private RelativeLayout splashScreen;

    public void closeCamera() {
        if(mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mHolder.removeCallback(this);
            mCamera.release();
        }
    }

    public void linkOverlay(SurfaceOverlay _overlay) {
        overlay = _overlay;

        double overlayScale = defineOverlayScale();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT &&
                !Build.FINGERPRINT.startsWith("generic"))
            // once the overlay is set I can open the camera
            overlay.getHolder().setFixedSize((int)(overlayScale*cameraHeight), (int)(overlayScale*cameraWidth));
        else
            overlay.getHolder().setFixedSize((int)overlayScale*cameraWidth, (int)overlayScale*cameraHeight);
        overlay.setScale(overlayScale);

    }

    private double defineOverlayScale() {
        int screen_height = getResources().getDisplayMetrics().heightPixels;
        if (screen_height > 2*cameraWidth)
            return 2.0;
        else
            return 1.0;
    }

    public CameraPreviewSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCameraPreviewSurface();
    }

    public CameraPreviewSurface(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initCameraPreviewSurface();
    }

    private void initCameraPreviewSurface() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mCamDetector.endSpoof();
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        // first, show us some info
        Log.v("SIZE", "Screen-size: "+w+" "+h);
        Log.v("SIZE", "aspect ratio: "+(double)h/w);
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        System.exit(10);

        return optimalSize;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamDetector.endSpoof();
        try {
            if (null == mCamera) {
                mCamera = CameraUtils.openFrontFacingCameraGingerbread();
            }
            else {
                mCamera.release();
                mCamera = CameraUtils.openFrontFacingCameraGingerbread();
            }
            mCamera.setPreviewDisplay(mHolder);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(cameraWidth, cameraHeight);
            mCamera.setParameters(parameters);

            mCamera.startPreview();

            int w = mCamera.getParameters().getPreviewSize().width;
            int h = mCamera.getParameters().getPreviewSize().height;
            mCamDetector.setSize(w, h);
            mCamera.setPreviewCallback(mCamDetector);
            splashScreen.setVisibility(View.INVISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "Unable to open camera or set preview display!");
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                              int height) {
        mCamDetector.endSpoof();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamDetector.endSpoof();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setTextView(TextView textView) {
        this.textView = textView;
    }

    public void setSplashScreen(RelativeLayout splashScreen) {
        this.splashScreen = splashScreen;
    }


    public class CameraDetectorCaller implements Camera.PreviewCallback{
        public static final String TAG = "CameraDetectorCaller";
        private double fps;
        private long lastTime;
        private long lastTest = System.nanoTime();
        private boolean testingSubject = false;
        private int spoofResult = 0;
        private Ava detector = new Ava();
        private int w=100, h=100;

        public void endSpoof() { detector.endSpoofDetection(); }

        public void setSize(int W, int H) { w=W; h=H; detector.endSpoofDetection(); }

        public void onPreviewFrame(byte[] data, Camera cam) {
            lastTime = System.nanoTime();
            //just to simulate a frontal camera :-) in case of emulator
            if (Build.FINGERPRINT.startsWith("generic")) {
                data = CameraUtils.rotateNV21(data, w, h, 90);
                int aux = h;
                h = w;
                w = aux;
            }

            if(System.nanoTime() - lastTest < 1000000000.0*5) {
                Ava.FaceAndLandmarks face_and_landmarks = detector.detectLargestFaceAndLandmarks(data, w, h, camType);
                Rect det = face_and_landmarks.face_;
                List<Point> landmarks = face_and_landmarks.landmarks_;

                fps = 1000000000.0 / (System.nanoTime() - lastTime);
                if (overlay != null) {
                    overlay.setFPS(fps);
                    overlay.setRectangle(det);
                    overlay.setPoints(landmarks);
                    overlay.setSpoofResult(spoofResult);
                    overlay.setBlinks(0, 0);
                }
                return;
            }
            else {
                overlay.hideResult();
                testingSubject = true;
                Ava.FaceLandmarksSpoof face_and_landmarks = detector.spoofDetection(data, w, h, camType);
                Rect det = face_and_landmarks.face_;
                List<Point> landmarks = face_and_landmarks.landmarks_;
                spoofResult = 0;
                if(face_and_landmarks.status_ != Ava.SpoofStatus.PROCESSING) {
                    if(face_and_landmarks.status_ == Ava.SpoofStatus.REAL_PERSON) {
                        spoofResult = 1;
                        lastTest = System.nanoTime();
                    }
                    else if(face_and_landmarks.status_ == Ava.SpoofStatus.FRAUD) {
                        spoofResult = 2;
                        lastTest = System.nanoTime();
                    }
                    else if(face_and_landmarks.status_ == Ava.SpoofStatus.SHAKING)
                        spoofResult = 3;
                    else
                        spoofResult = 4;

                    testingSubject = false;
                }

                if(spoofResult == 0) {
                    textView.setText("Analyzing face...");
                }
                if(spoofResult == 1) {
                    textView.setText("Valid face!");
                }
                if(spoofResult == 2) {
                    textView.setText("Invalid face!");
                }
                if(spoofResult == 3) {
                    textView.setText("Camera shake");
                }
                if(spoofResult == 4) {
                    textView.setText("Face too far...");
                }

                fps = 1000000000.0 / (System.nanoTime() - lastTime);
                if (overlay != null) {
                    overlay.setFPS(fps);
                    overlay.setRectangle(det);
                    overlay.setPoints(landmarks);

                    // if the result changed to invalid or valid, set an image to the overlay
                    if ((spoofResult == 1 || spoofResult == 2) && overlay.getSpoofResult()!=spoofResult) {
                        displayResultOnOverlay(data);
                    }
                    overlay.setSpoofResult(spoofResult);
                }
            }
        }

        void displayResultOnOverlay(byte[] data) {
            int[] rgb_data = new int[cameraWidth * cameraHeight];
            CameraUtils.YUV_NV21_TO_RGB(rgb_data, data, cameraWidth, cameraHeight);

            Bitmap bitmap = Bitmap.createBitmap(cameraWidth, cameraHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(rgb_data, 0, cameraWidth, 0, 0, cameraWidth, cameraHeight);

            Matrix matrix = new Matrix();
            if (camType == Ava.CameraType.FRONT_CAMERA) {
                matrix.preScale(1.0f, -1.0f); // flip horizontally
                matrix.postRotate(-90);
            }
            else
                matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            overlay.setSpoofResult(spoofResult);
            overlay.showResult(rotated);

        }
    }

    void changeCamera() {
        // first stop the current camera
        if(mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mHolder.removeCallback(this);
            mCamera.release();
        }
        mCamera = null;

        try {
            if (camType == Ava.CameraType.BACK_CAMERA) {
                mCamera = CameraUtils.openFrontFacingCameraGingerbread();
                camType = Ava.CameraType.FRONT_CAMERA;
            }
            else {
                mCamera = CameraUtils.openBackFacingCameraGingerbread();
                camType = Ava.CameraType.BACK_CAMERA;
            }
            int w = cameraWidth;
            int h = cameraHeight;
            mCamera.setPreviewDisplay(mHolder);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(cameraWidth, cameraHeight);
            mCamera.setParameters(parameters);

            mCamera.startPreview();
            mCamDetector.setSize(w, h);
            mCamera.setPreviewCallback(mCamDetector);

        } catch (IOException e) {
            Log.e(TAG, "Unable to open camera or set preview display!");
            mCamera.release();
            mCamera = null;
        }

    }


}
