package br.com.meerkat.avapreview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

import static java.util.Arrays.fill;

/**
 * Created by gfuhr on 5/4/16.
 */
public class SurfaceOverlay extends SurfaceView implements SurfaceHolder.Callback{
    private static final String TAG = "SurfaceOverlay";
    private int spoofResult;
    private Rect detection;
    private List<Point> landmarks;
    private float[] blinks = new float[40];
    private float[] blinks2 = new float[40];
    private int curr_blink = 0;
    private DrawingThread drawingThread;
    private double[] FPS = new double[10];
    private int frameCount = 0;
    private SurfaceHolder mHolder;
    private double scale = 1.0;


//
//    public SurfaceOverlay(Context context) {
//        super(context);
//        initSurfaceOverlay();
//    }

    public SurfaceOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSurfaceOverlay();
    }

    public SurfaceOverlay(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initSurfaceOverlay();
    }

    private void initSurfaceOverlay() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        fill(FPS, 0.0);
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setSpoofResult(int spoofResult) { this.spoofResult = spoofResult; }

    public void setBlinks(float blink1, float blink2) {
        if (curr_blink >= blinks.length)
            curr_blink = curr_blink % blinks.length;

        blinks[curr_blink] = blink1;
        blinks2[curr_blink] = blink2;
        curr_blink++;
    }

    class DrawingThread extends Thread {

        private boolean mRun;

        public void setRunning(boolean b) {
            mRun = b;
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mHolder.lockCanvas(null);
                    synchronized (mHolder) {
                        doDraw(c);
                        sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (c != null) {
                        mHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        private void doDraw(Canvas canvas) {
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                Paint paint_spoof = new Paint();
                if(spoofResult == 0)  paint_spoof.setColor(Color.WHITE);
                if(spoofResult == 1)  paint_spoof.setColor(Color.GREEN);
                if(spoofResult == 2)  paint_spoof.setColor(Color.RED);
                if(spoofResult == 3)  paint_spoof.setColor(Color.CYAN);
                if(spoofResult == 4)  paint_spoof.setColor(Color.BLUE);
                paint_spoof.setStrokeWidth(60);
                canvas.drawLine(0, 0, canvas.getWidth(), 0, paint_spoof);
                canvas.drawLine(canvas.getWidth(), 0, canvas.getWidth(), canvas.getHeight(), paint_spoof);
                canvas.drawLine(canvas.getWidth(), canvas.getHeight(), 0, canvas.getHeight(), paint_spoof);
                canvas.drawLine(0, canvas.getHeight(), 0, 0, paint_spoof);

                Paint paint = new Paint();

                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.LEFT);

                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(40);
                canvas.drawText("FPS:" + String.format("%.2f", getFPS()), 60, 60, paint);

                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(8);

                // Draw blinking debug
//                int   dx = canvas.getWidth()/(blinks.length+3);
//                float dy = 10*60.0f;
//                for(int i=0; i<blinks.length-1; i++) {
//                    int x1 = dx*(i+1);
//                    int x2 = dx*(i+2);
//                    canvas.drawLine(x1, (int)(blinks[i]*dy) + 300,
//                            x2, (int)(blinks[i+1]*dy) + 300, paint);
//
//                    canvas.drawLine(x1, (int)(blinks2[i]*dy) + 150,
//                            x2, (int)(blinks2[i+1]*dy) + 150, paint);
//                }

                paint.setStyle(Paint.Style.STROKE);
                paint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));

                if (detection != null) {
                    canvas.drawRect(detection, paint);
                }

//                for(int i=0; i<landmarks.size(); i=i++)
//                    canvas.drawPoint(landmarks.get(i).x, landmarks.get(i).y, paint_spoof);

                // Draw landmarks mask
                if (landmarks != null) {
                    Paint paint_pt = new Paint();
                    paint_pt.setStyle(Paint.Style.FILL);
                    paint_pt.setColor(Color.rgb(255,255,255));
                    paint_pt.setStrokeWidth(10);

                    for(int i=0; i<landmarks.size(); i++) {
                        Point land = landmarks.get(i);
                        canvas.drawCircle(land.x, land.y, 4, paint_pt);
                    }
                    
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated!");
        holder.setFormat(PixelFormat.RGBA_8888);

        this.setZOrderOnTop(true);
        drawingThread = new DrawingThread();
        drawingThread.setRunning(true);
        drawingThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void setRectangle(Rect det) {
        this.detection = det;
        if (scale != 1.0)
            detection.set((int)(detection.left*scale), (int)(detection.top*scale), (int)(detection.right*scale), (int)(detection.bottom*scale));
    }

    public void setPoints(List<Point> pts) {
        this.landmarks = pts;
        if (scale != 1.0) {
            for (Point p : pts) {
                p.x = (int)(p.x*scale);
                p.y = (int)(p.y*scale);
            }
        }
    }

    public double getFPS() {
        double sum = 0;
        for (double d : FPS) sum += d;

        return sum/FPS.length;
    }

    public void setFPS(double fps) {
        this.FPS[frameCount % FPS.length] = fps;
        frameCount++;
    }
}
