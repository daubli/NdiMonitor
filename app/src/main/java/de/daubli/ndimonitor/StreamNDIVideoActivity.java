package de.daubli.ndimonitor;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.daubli.ndimonitor.R;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import androidx.appcompat.app.AppCompatActivity;
import de.daubli.ndimonitor.ndi.NdiSource;
import de.daubli.ndimonitor.settings.SettingsStore;
import de.daubli.ndimonitor.view.focusassist.FocusPeakingOverlayView;
import de.daubli.ndimonitor.view.FramingHelperOverlayView;
import de.daubli.ndimonitor.view.VideoView;
import de.daubli.ndimonitor.view.zebra.ZebraOverlayView;

public class StreamNDIVideoActivity extends AppCompatActivity {
    NdiSource ndiVideoNdiSource;
    VideoView videoView;

    FramingHelperOverlayView ndiFramingHelperOverlayView;

    FocusPeakingOverlayView focusPeakingOverlayView;

    ZebraOverlayView zebraOverlayView;

    LinearLayout menuLayout;

    ImageButton zebraButton;
    ImageButton toggleFocusAssistButton;
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
        videoView = findViewById(R.id.videoView);
        ndiFramingHelperOverlayView = findViewById(R.id.framingHelperOverlayView);
        focusPeakingOverlayView = findViewById(R.id.focusPeakingOverlayView);
        zebraOverlayView = findViewById(R.id.zebraOverlayView);
        menuLayout = findViewById(R.id.menu_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initMenu();
        setFullScreen();
        initSavedState();
    }

    private void initMenu() {
        initializeZebraButton();
        initializeToggleFocusAssistButton();
        initializeToggleGridButton();
        initializeCloseButton();
        initShowHideMenu();
    }

    private void initSavedState() {
        SettingsStore settingsStore = new SettingsStore();
        if (settingsStore.isFramingHelperOverlayEnabled()) {
            ndiFramingHelperOverlayView.setVisibility(View.VISIBLE);
            toggleGridButton.setImageResource(R.drawable.grid_icon_3x3_selected);
            ndiFramingHelperOverlayView.toggleFramingHelper();
        }
        if (settingsStore.isFocusAssistEnabled()) {
            focusPeakingOverlayView.setVisibility(View.VISIBLE);
            toggleFocusAssistButton.setImageResource(R.drawable.focus_assist_selected);
        }
        if (settingsStore.isZebraEnabled()) {
            zebraOverlayView.setVisibility(View.VISIBLE);
            zebraButton.setImageResource(R.drawable.zebra_selected);
        }
    }

    private void initShowHideMenu() {
        this.menuHandler = new Handler(Looper.getMainLooper());
        this.hideMenuCallback = () -> menuLayout.setVisibility(View.GONE);
        videoView.setOnClickListener(view -> {
            menuLayout.setVisibility(View.VISIBLE);
            menuHandler.removeCallbacks(hideMenuCallback);
            menuHandler.postDelayed(hideMenuCallback, 5000);
        });
    }

    private void initializeZebraButton() {
        this.zebraButton = findViewById(R.id.zebra_button);
        this.zebraButton.setOnClickListener(view -> {
            SettingsStore settingsStore = new SettingsStore();
            if (zebraOverlayView.getVisibility() == View.VISIBLE) {
                zebraOverlayView.setVisibility(View.GONE);
                zebraButton.setImageResource(R.drawable.zebra);
                settingsStore.setZebraEnabled(false);
            } else {
                zebraOverlayView.setVisibility(View.VISIBLE);
                zebraButton.setImageResource(R.drawable.zebra_selected);
                settingsStore.setZebraEnabled(true);
            }
        });
    }

    private void initializeToggleFocusAssistButton() {
        this.toggleFocusAssistButton = findViewById(R.id.focus_assist_button);
        this.toggleFocusAssistButton.setOnClickListener(view -> {
            SettingsStore settingsStore = new SettingsStore();
            if (focusPeakingOverlayView.getVisibility() == View.VISIBLE) {
                focusPeakingOverlayView.setVisibility(View.GONE);
                toggleFocusAssistButton.setImageResource(R.drawable.focus_assist);
                settingsStore.setFocusAssistEnabled(false);
            } else {
                focusPeakingOverlayView.setVisibility(View.VISIBLE);
                toggleFocusAssistButton.setImageResource(R.drawable.focus_assist_selected);
                settingsStore.setFocusAssistEnabled(true);
            }
        });
    }

    private void initializeToggleGridButton() {
        this.toggleGridButton = findViewById(R.id.grid_button);
        this.toggleGridButton.setOnClickListener(view -> {
            SettingsStore settingsStore = new SettingsStore();
            if (ndiFramingHelperOverlayView.getVisibility() == View.VISIBLE) {
                ndiFramingHelperOverlayView.setVisibility(View.GONE);
                toggleGridButton.setImageResource(R.drawable.grid_icon_3x3);
                settingsStore.setFramingHelperOverlayEnabled(false);
            } else {
                ndiFramingHelperOverlayView.setVisibility(View.VISIBLE);
                toggleGridButton.setImageResource(R.drawable.grid_icon_3x3_selected);
                settingsStore.setFramingHelperOverlayEnabled(true);
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
        runner = new StreamNDIVideoRunner(ndiVideoNdiSource, videoView, ndiFramingHelperOverlayView, focusPeakingOverlayView, zebraOverlayView, this);
        runner.start();
    }
}
