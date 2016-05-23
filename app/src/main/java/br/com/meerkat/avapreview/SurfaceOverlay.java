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


        Log.v(TAG, "blink "+Float.toString(blink1)+" on position "+String.valueOf(curr_blink));
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
                        sleep(30);
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
                if(spoofResult == 3)  paint_spoof.setColor(Color.BLUE);
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
                canvas.drawText("FPS:" + String.format("%.2f", getFPS()), 10, 50, paint);

                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(8);

                // Draw blinking debug
                int   dx = canvas.getWidth()/(blinks.length+3);
                float dy = 10*60.0f;
                for(int i=0; i<blinks.length-1; i++) {
                    int x1 = dx*(i+1);
                    int x2 = dx*(i+2);
                    canvas.drawLine(x1, (int)(blinks[i]*dy) + 300,
                            x2, (int)(blinks[i+1]*dy) + 300, paint);

                    canvas.drawLine(x1, (int)(blinks2[i]*dy) + 150,
                            x2, (int)(blinks2[i+1]*dy) + 150, paint);
                }

                paint.setStyle(Paint.Style.STROKE);
                paint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));

                if (detection != null) {
                    canvas.drawRect(detection, paint);
                }

                // Draw landmarks mask
                if (landmarks != null) {
                    if(landmarks.size() < 68)
                        return;
                    Paint paint_pt = new Paint();
                    paint_pt.setStyle(Paint.Style.FILL);
                    paint_pt.setColor(Color.rgb(255,255,255));
                    paint_pt.setStrokeWidth(6);

                    Paint paint_mask = new Paint();
                    paint_mask.setStyle(Paint.Style.FILL);
                    paint_mask.setColor(Color.rgb(200,200,200));
                    paint_mask.setStrokeWidth(2);

                    Point pi1 = new Point((landmarks.get(55).x+landmarks.get(56).x)/2,
                            (landmarks.get(55).y+landmarks.get(56).y)/2);
                    Point pi2 = new Point((landmarks.get(63).x+landmarks.get(65).x)/2,
                            (landmarks.get(63).y+landmarks.get(65).y)/2);
                    Point pi3 = new Point((landmarks.get(54).x+landmarks.get(14).x)/2,
                            (landmarks.get(54).y+landmarks.get(14).y)/2);

                    Point pi4 = new Point((landmarks.get(58).x+landmarks.get(59).x)/2,
                            (landmarks.get(58).y+landmarks.get(59).y)/2);
                    Point pi5 = new Point((landmarks.get(61).x+landmarks.get(67).x)/2,
                            (landmarks.get(61).y+landmarks.get(67).y)/2);
                    Point pi6 = new Point((landmarks.get(48).x+landmarks.get(2).x)/2,
                            (landmarks.get(48).y+landmarks.get(2).y)/2);

                    Point[] landmarks2 = new Point[68+6];
                    for(int i=0; i<landmarks.size(); i++)
                        landmarks2[i] = landmarks.get(i);

                    int p1=68, p2=69, p3=70, p4=71, p5=72, p6=73;
                    landmarks2[p1] = pi1; landmarks2[p2] = pi2; landmarks2[p3] = pi3;
                    landmarks2[p4] = pi4; landmarks2[p5] = pi5; landmarks2[p6] = pi6;

                    int[] points_left = {8,57, 57,10, 10,8, 57,p1, p1,10, 57,p2, p2,p1, 51,52, 52,p2,
                            p2,51, 52,54, 54,p2, p2,54, 54,p1, p1,54, 54,10, 54,p3, p3,10, p3,10,
                            10,12, p3,12, 12,14, 14,p3};

                    int[] points_right = {p2,p5, 57,6, 6,8, 57,p4, p4,6, 57,p5, p5,p4, 51,50, 50,p5,
                            p5,51, 50,48, 48,p5, p5,48, 48,p4, p4,48, 48,6, 48,p6, p6,6, p6,6,
                            6,4, p6,4, 4,2, 2,p6};

                    int[] top_right = {51,33, 33,53, 33,30, 30,35, 35,33, 30,27, 27,42, 42,30,
                            27,22, 22,42, 42,27, 22,43, 43,42, 42,35, 35,52, 42,47, 47,35, 47,46,
                            46,35, 35,46, 46,p3, p3,35, 35,p3, p3,54, 54,35, 46,14, 14,16, 16,46,
                            46,45, 45,16, 16,26, 26,44, 44,45, 44,26, 26,24, 24,44, 44,43,
                            43,24, 24,22, 22,43, 43,42, 42,22, 22,27};

                    int[] top_left = {33,50, 30,31, 31,33, 27,39, 39,30, 27,21, 21,39, 39,27,
                            21,38, 38,39, 39,31, 31,50, 39,40, 40,31, 40,41, 41,31, 31,41,
                            41,p6, p6,31, 31,p6, p6,48, 48,31, 41,2, 2,0, 41,36, 36,0, 0,17, 17,37,
                            37,36, 37,17, 17,19, 19,37, 37,38, 38,19, 19,21, 21,38, 38,39, 39,21,
                            21,27, 21,22};

                    for(int i=0; i<points_left.length; i=i+2) {
                        int idx = points_left[i];
                        int idx2 = points_left[i+1];
                        canvas.drawLine(landmarks2[idx].x, landmarks2[idx].y,
                                        landmarks2[idx2].x, landmarks2[idx2].y, paint_mask);
                        canvas.drawPoint(landmarks2[idx].x, landmarks2[idx].y, paint_pt);
                        canvas.drawPoint(landmarks2[idx2].x, landmarks2[idx2].y, paint_pt);
                    }

                    for(int i=0; i<points_right.length; i=i+2) {
                        int idx = points_right[i];
                        int idx2 = points_right[i+1];
                        canvas.drawLine(landmarks2[idx].x, landmarks2[idx].y,
                                landmarks2[idx2].x, landmarks2[idx2].y, paint_mask);
                        canvas.drawPoint(landmarks2[idx].x, landmarks2[idx].y, paint_pt);
                        canvas.drawPoint(landmarks2[idx2].x, landmarks2[idx2].y, paint_pt);
                    }

                    for(int i=0; i<top_right.length; i=i+2) {
                        int idx = top_right[i];
                        int idx2 = top_right[i+1];
                        canvas.drawLine(landmarks2[idx].x, landmarks2[idx].y,
                                landmarks2[idx2].x, landmarks2[idx2].y, paint_mask);
                        canvas.drawPoint(landmarks2[idx].x, landmarks2[idx].y, paint_pt);
                        canvas.drawPoint(landmarks2[idx2].x, landmarks2[idx2].y, paint_pt);
                    }

                    for(int i=0; i<top_left.length; i=i+2) {
                        int idx = top_left[i];
                        int idx2 = top_left[i+1];
                        canvas.drawLine(landmarks2[idx].x, landmarks2[idx].y,
                                landmarks2[idx2].x, landmarks2[idx2].y, paint_mask);
                        canvas.drawPoint(landmarks2[idx].x, landmarks2[idx].y, paint_pt);
                        canvas.drawPoint(landmarks2[idx2].x, landmarks2[idx2].y, paint_pt);
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
