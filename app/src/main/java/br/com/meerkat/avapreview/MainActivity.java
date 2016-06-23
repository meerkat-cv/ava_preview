package br.com.meerkat.avapreview;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageButton;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import br.com.meerkat.ava.Ava;

public class MainActivity extends Activity {
    private CameraPreviewSurface preview;
    private SurfaceOverlay overlay;


    private static final int REQUEST_CAMERA_RESULT = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 112;
    private static final String TAG = "MainActivity";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private RelativeLayout aboutLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // should request permission if android api > 23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasWritePermission = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (hasWritePermission == false) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            }
            Ava.copyLandmarkModel(this);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_RESULT);
            } else {
                setContentView(R.layout.activity_main);

                TextView t2 = (TextView) findViewById(R.id.aboutTextView);
                t2.setMovementMethod(LinkMovementMethod.getInstance());

                aboutLayout = (RelativeLayout) findViewById(R.id.relativeLayoutAbout);
                aboutLayout.setVisibility(View.INVISIBLE);

                overlay = (SurfaceOverlay) findViewById(R.id.surfaceOverlayView);
                preview = (CameraPreviewSurface) findViewById(R.id.surfaceView);
                preview.linkOverlay(overlay);
                preview.setTextView((TextView)findViewById(R.id.statusText));
            }
        } else {
            Ava.copyLandmarkModel(this);

            setContentView(R.layout.activity_main);
            TextView t2 = (TextView) findViewById(R.id.aboutTextView);
            t2.setMovementMethod(LinkMovementMethod.getInstance());
//            TextView t2 = (TextView) findViewById(R.id.aboutTextView);
//            Linkify.addLinks(t2, Linkify.WEB_URLS);

            aboutLayout = (RelativeLayout) findViewById(R.id.relativeLayoutAbout);
            aboutLayout.setVisibility(View.INVISIBLE);
            overlay = (SurfaceOverlay) findViewById(R.id.surfaceOverlayView);
            preview = (CameraPreviewSurface) findViewById(R.id.surfaceView);
            preview.linkOverlay(overlay);
            preview.setTextView((TextView) findViewById(R.id.statusText));
        }

        final ImageButton button = (ImageButton) findViewById(R.id.changeCamButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "Change camera Button clicked!");
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_RESULT:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Cannot run application because camera service permission have not been granted", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        preview.closeCamera();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
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
        AppIndex.AppIndexApi.start(client, viewAction);
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
    }
}
