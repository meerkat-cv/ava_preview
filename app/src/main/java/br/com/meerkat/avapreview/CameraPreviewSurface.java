package br.com.meerkat.avapreview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.res.Configuration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.com.meerkat.ava.Ava;

/**
 * Created by meerkat on 4/29/16.
 */
public class CameraPreviewSurface extends SurfaceView implements SurfaceHolder.Callback{
    private Ava.CameraType camType = Ava.CameraType.FRONT_CAMERA;
    private int cameraWidth = 640;
    private int cameraHeight = 480;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private CameraDetectorCaller mCamDetector = new CameraDetectorCaller();
    public static final String TAG = "CameraPreviewSurface";
    public SurfaceOverlay overlay;


    public void linkOverlay(SurfaceOverlay _overlay) {
        Log.v(TAG, "overlay is null: "+_overlay);
        overlay = _overlay;

        Log.v(TAG, "orientation:" + getResources().getConfiguration().orientation);
        double overlayScale = 2;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT &&
                !Build.FINGERPRINT.startsWith("generic"))
            // once the overlay is set I can open the camera
            overlay.getHolder().setFixedSize((int)(overlayScale*cameraHeight), (int)(overlayScale*cameraWidth));
        else
            overlay.getHolder().setFixedSize((int)overlayScale*cameraWidth, (int)overlayScale*cameraHeight);
        overlay.setScale(overlayScale);

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
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (null == mCamera) {
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

        } catch (IOException e) {
            Log.e(TAG, "Unable to open camera or set preview display!");
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                              int height) {
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
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

        public void setSize(int W, int H) { w=W; h=H; }

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

                fps = 1000000000.0 / (System.nanoTime() - lastTime);
                if (overlay != null) {
                    overlay.setFPS(fps);
                    overlay.setRectangle(det);
                    overlay.setPoints(landmarks);
                    overlay.setSpoofResult(spoofResult);
                }
            }
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
