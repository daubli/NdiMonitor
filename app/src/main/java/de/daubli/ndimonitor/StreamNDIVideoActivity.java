package de.daubli.ndimonitor;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Logger;

import com.daubli.ndimonitor.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.app.Activity;
import android.content.Intent;
import android.media.*;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import de.daubli.ndimonitor.view.NdiVideoView;
import me.walkerknapp.devolay.*;

public class StreamNDIVideoActivity extends AppCompatActivity implements View.OnClickListener {

    DevolaySource ndiVideoSource;
    NdiVideoView ndiVideoView;
    FloatingActionButton closeButton;

    private StreamNDIVideoRunner runner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ndiVideoSource = MainActivity.getSource();
        setContentView(R.layout.stream_ndi_video_activity);
        ndiVideoView = findViewById(R.id.ndiVideoView);
        closeButton = findViewById(R.id.regular_fab);
        closeButton.setOnClickListener(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        runner = new StreamNDIVideoRunner(ndiVideoSource, ndiVideoView, this);
        runner.start();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.regular_fab && runner != null) {
            runner.shutdown();
            while (runner != null) {
                try {
                    runner.join();
                    runner = null;
                    this.finish();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
