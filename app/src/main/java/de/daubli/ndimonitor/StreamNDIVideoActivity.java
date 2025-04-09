package de.daubli.ndimonitor;

import com.daubli.ndimonitor.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import androidx.appcompat.app.AppCompatActivity;
import de.daubli.ndimonitor.ndi.Source;
import de.daubli.ndimonitor.view.NdiVideoView;

public class StreamNDIVideoActivity extends AppCompatActivity {

    Source ndiVideoSource;
    NdiVideoView ndiVideoView;
    FloatingActionButton closeButton;

    private StreamNDIVideoRunner runner;

    private Handler closeButtonHideHandler;
    private Runnable hideCloseButtonCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ndiVideoSource = MainActivity.getSource();
        setContentView(R.layout.stream_ndi_video_activity);
        ndiVideoView = findViewById(R.id.ndiVideoView);
        closeButton = findViewById(R.id.regular_fab);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initializeCloseButton();
        setFullScreen();
    }

    private void initializeCloseButton() {
        this.closeButton = findViewById(R.id.regular_fab);
        this.closeButton.setOnClickListener(view -> closeVideoActivity());
        this.closeButtonHideHandler = new Handler(Looper.getMainLooper());
        this.hideCloseButtonCallback = () -> closeButton.setVisibility(View.GONE);
        attachCloseButtonShowAndHideHandler();
    }

    private void closeVideoActivity() {
        runner.shutdown();
    }

    private void attachCloseButtonShowAndHideHandler() {
        ndiVideoView.setOnClickListener(view -> {
            closeButton.setVisibility(View.VISIBLE);
            closeButtonHideHandler.removeCallbacks(hideCloseButtonCallback);
            closeButtonHideHandler.postDelayed(hideCloseButtonCallback, 5000);
        });
    }

    private void setFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            //noinspection deprecation
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        runner = new StreamNDIVideoRunner(ndiVideoSource, ndiVideoView, this);
        runner.start();
    }
}
