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

        public void onPreviewFrame(byte[] data, Camera cam) {
            lastTime = System.nanoTime();
            int w = cam.getParameters().getPreviewSize().width;
            int h = cam.getParameters().getPreviewSize().height;

            Log.v(TAG, "Frame size: " + w +     " " + h);


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
                Log.v(TAG, "faceDetection"+det);

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
                Ava.FaceLandmarksBlink face_and_landmarks = detector.blinkActivity(data, w, h, camType);
                Rect det = face_and_landmarks.face_;
                List<Point> landmarks = face_and_landmarks.landmarks_;
                Log.v(TAG, "faceDetection" + det);
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
                    else
                        spoofResult = 3;

                    testingSubject = false;
                }

                fps = 1000000000.0 / (System.nanoTime() - lastTime);
                if (overlay != null) {
                    overlay.setFPS(fps);
                    overlay.setRectangle(det);
                    overlay.setPoints(landmarks);
                    overlay.setSpoofResult(spoofResult);
                    overlay.setBlinks(getBlink(landmarks), face_and_landmarks.conf_);
                }
            }
        }
    }


    private float getBlink(List<Point> landmarks) {
        double blink = 0.0f;
        if(landmarks.size() > 0) {
            double dx_h = landmarks.get(39).x - landmarks.get(36).x;
            double dy_h = landmarks.get(39).y - landmarks.get(36).y;
            double dx_v = landmarks.get(41).x - landmarks.get(37).x;
            double dy_v = landmarks.get(41).y - landmarks.get(37).y;

            blink = Math.sqrt(dx_v*dx_v + dy_v*dy_v)/Math.sqrt(dx_h*dx_h + dy_h*dy_h);
            Log.v(TAG, "Not zero!! "+String.valueOf(dx_h)+" with blink: "+String.valueOf(blink));
        }

        return (float)blink;
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
            mCamera.setPreviewDisplay(mHolder);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(cameraWidth, cameraHeight);
            mCamera.setParameters(parameters);

            mCamera.startPreview();
            mCamera.setPreviewCallback(mCamDetector);

        } catch (IOException e) {
            Log.e(TAG, "Unable to open camera or set preview display!");
            mCamera.release();
            mCamera = null;
        }

    }


}
