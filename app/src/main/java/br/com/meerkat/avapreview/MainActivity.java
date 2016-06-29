package br.com.meerkat.avapreview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;

import br.com.meerkat.ava.Ava;



public class MainActivity extends Activity {
    // this should only be true if the google-services.json is provided in root folder

    private CameraPreviewSurface preview = null;
    private SurfaceOverlay overlay;

    private static final int REQUEST_CAMERA_RESULT = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 112;
    private static final String TAG = "MainActivity";
    private Tracker mTracker;
    private static long mUptime;
    private static long mLoadTime;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private RelativeLayout aboutLayout;
    private FrameLayout pnlFlash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLoadTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);

        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        boolean hasWritePermission = false;
        boolean hasCameraPermission = false;

        // should request permission if android api > 23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasWritePermission = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

            if (hasWritePermission == false) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);

                while (hasWritePermission == false) {
                    try {
                        Thread.sleep(50);                 //1000 milliseconds is one second.
                        hasWritePermission = (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED);
            if (hasCameraPermission == false) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_RESULT);

                while (hasCameraPermission == false) {
                    try {
                        Thread.sleep(50);                 //1000 milliseconds is one second.
                        hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        try {
            Ava.copyModelFiles(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        TextView t2 = (TextView) findViewById(R.id.aboutTextView);
        t2.setMovementMethod(LinkMovementMethod.getInstance());

        aboutLayout = (RelativeLayout) findViewById(R.id.relativeLayoutAbout);
        aboutLayout.setVisibility(View.INVISIBLE);
        overlay = (SurfaceOverlay) findViewById(R.id.surfaceOverlayView);
        preview = (CameraPreviewSurface) findViewById(R.id.surfaceView);
        preview.linkOverlay(overlay);
        preview.setTextView((TextView) findViewById(R.id.statusText));


        pnlFlash = (FrameLayout) findViewById(R.id.pnlFlash);
        overlay.setFlashPanel(pnlFlash);

        FrameLayout resultLayout = (FrameLayout) findViewById(R.id.resultScreen);
        overlay.setResultLayout(resultLayout);

        ImageView resultImageView = (ImageView) findViewById(R.id.resultFace);
        overlay.setResultImageView(resultImageView);

        RelativeLayout splashScreen = (RelativeLayout) findViewById(R.id.splashScreen);
        preview.setSplashScreen(splashScreen);
        
        final ImageButton button = (ImageButton) findViewById(R.id.changeCamButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                preview.changeCamera();
            }
        });

        final ImageButton buttonMeerkat = (ImageButton) findViewById(R.id.meerkatButton);
        buttonMeerkat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                aboutLayout.setVisibility(View.VISIBLE);
            }
        });

        final ImageButton buttonCloseWindow = (ImageButton) findViewById(R.id.closeWindowButton);
        buttonCloseWindow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                aboutLayout.setVisibility(View.INVISIBLE);
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        Log.i(TAG, "Loading finished");
        mTracker.send(new HitBuilders.TimingBuilder()
                .setCategory("Action")
                .setLabel("Load_finished")
                .setValue(System.currentTimeMillis()-mLoadTime)
                .build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_RESULT:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Cannot run application because camera service permission have not been granted", Toast.LENGTH_SHORT).show();
                }
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Cannot run application because write permission have not been granted", Toast.LENGTH_SHORT).show();
                }
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(preview != null) {
            preview.closeCamera();
            preview = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://br.com.meerkat.avapreview/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);

        mUptime = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUptime = System.currentTimeMillis();
    }


    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG, "Pausing");
        mTracker.send(new HitBuilders.TimingBuilder()
                .setCategory("Action")
                .setLabel("Pausing")
                .setValue(System.currentTimeMillis()-mUptime)
                .build());
    }

    @Override
    public void onStop() {

        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://br.com.meerkat.avapreview/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();

        Log.i(TAG, "Stopping");
        mTracker.send(new HitBuilders.TimingBuilder()
                .setCategory("Action")
                .setLabel("Stopping")
                .setValue(System.currentTimeMillis() - mUptime)
                .build());
    }



}
