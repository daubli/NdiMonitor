package de.daubli.ndimonitor;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.daubli.ndimonitor.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import androidx.appcompat.app.AppCompatActivity;
import de.daubli.ndimonitor.ndi.NdiSource;
import de.daubli.ndimonitor.view.FramingHelperOverlayView;
import de.daubli.ndimonitor.view.NdiVideoView;

public class StreamNDIVideoActivity extends AppCompatActivity {
    NdiSource ndiVideoNdiSource;
    NdiVideoView ndiVideoView;

    FramingHelperOverlayView ndiFramingHelperOverlayView;
    LinearLayout menuLayout;

    ImageButton toggleGridButton;
    ImageButton closeButton;
    private StreamNDIVideoRunner runner;
    private Handler menuHandler;
    private Runnable hideMenuCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ndiVideoNdiSource = MainActivity.getSource();
        setContentView(R.layout.stream_ndi_video_activity);
        ndiVideoView = findViewById(R.id.ndiVideoView);
        ndiFramingHelperOverlayView = findViewById(R.id.framingHelperOverlayView);
//        closeButton = findViewById(R.id.regular_fab);
        menuLayout = findViewById(R.id.menu_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initializeToggleGridButton();
        initializeCloseButton();
        initShowHideMenu();
        setFullScreen();
    }

    private void initShowHideMenu() {
        this.menuHandler = new Handler(Looper.getMainLooper());
        this.hideMenuCallback = () -> menuLayout.setVisibility(View.GONE);
        ndiVideoView.setOnClickListener(view -> {
            menuLayout.setVisibility(View.VISIBLE);
            menuHandler.removeCallbacks(hideMenuCallback);
            menuHandler.postDelayed(hideMenuCallback, 5000);
        });
    }

    private void initializeToggleGridButton() {
        this.toggleGridButton = findViewById(R.id.grid_button);
        this.toggleGridButton.setOnClickListener(view -> {
            if (ndiFramingHelperOverlayView.getVisibility() == View.VISIBLE) {
                ndiFramingHelperOverlayView.setVisibility(View.GONE);
                toggleGridButton.setImageResource(R.drawable.grid_icon_3x3);
            } else {
                ndiFramingHelperOverlayView.setVisibility(View.VISIBLE);
                toggleGridButton.setImageResource(R.drawable.grid_icon_3x3_selected);
            }
            ndiFramingHelperOverlayView.toggleFramingHelper();
        });
    }

    private void initializeCloseButton() {
        this.closeButton = findViewById(R.id.close_button);
        this.closeButton.setOnClickListener(view -> closeVideoActivity());
    }

    private void closeVideoActivity() {
        runner.shutdown();
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
        runner = new StreamNDIVideoRunner(ndiVideoNdiSource, ndiVideoView, ndiFramingHelperOverlayView, this);
        runner.start();
    }
}
