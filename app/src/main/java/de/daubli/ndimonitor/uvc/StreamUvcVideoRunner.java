package de.daubli.ndimonitor.uvc;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import de.daubli.ndimonitor.StreamVideoActivity;
import de.daubli.ndimonitor.StreamVideoRunner;
import de.daubli.ndimonitor.decoder.MJpegBitmapBuilder;
import de.daubli.ndimonitor.view.FramingHelperOverlayView;
import de.daubli.ndimonitor.view.VideoView;
import de.daubli.ndimonitor.view.focusassist.FocusPeakingOverlayView;
import de.daubli.ndimonitor.view.zebra.ZebraOverlayView;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamUvcVideoRunner extends Thread implements StreamVideoRunner {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final VideoView videoView;
    private final FramingHelperOverlayView framingHelperOverlayView;
    private final FocusPeakingOverlayView focusPeakingOverlayView;
    private final ZebraOverlayView zebraOverlayView;
    private final StreamVideoActivity activity;

    private final UvcCaptureManager captureManager;
    private volatile boolean running = true;
    private Choreographer choreographer;

    private final AtomicBoolean frameAvailable = new AtomicBoolean(false);
    private ByteBuffer latestFrame;

    private final Runnable shutdownTask = () -> {
        if (choreographer != null) choreographer.removeFrameCallback(this.frameCallback);
    };

    public StreamUvcVideoRunner(UVCSource uvcSource,
                                VideoView videoView,
                                FramingHelperOverlayView framingHelperOverlayView,
                                FocusPeakingOverlayView focusPeakingOverlayView,
                                ZebraOverlayView zebraOverlayView,
                                StreamVideoActivity activity) {
        this.captureManager = new UvcCaptureManager(uvcSource, activity);
        this.videoView = videoView;
        this.framingHelperOverlayView = framingHelperOverlayView;
        this.zebraOverlayView = zebraOverlayView;
        this.focusPeakingOverlayView = focusPeakingOverlayView;
        this.activity = activity;
    }

    @Override
    public void run() {
        captureManager.start(data -> {
            latestFrame = ByteBuffer.wrap(data.clone());
            frameAvailable.set(true);
        });

        mainHandler.post(() -> {
            choreographer = Choreographer.getInstance();
            choreographer.postFrameCallback(frameCallback);
        });
    }

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!running) return;

            if (frameAvailable.getAndSet(false) && latestFrame != null) {
                Bitmap decodedFrame = MJpegBitmapBuilder.builder()
                        .withRawData(latestFrame)
                        .withWidth(videoView.getWidth())
                        .withHeight(videoView.getHeight())
                        .build();

                videoView.setVisibility(View.VISIBLE);
                videoView.updateFrame(decodedFrame);
                Rect dstRect = videoView.getLocationOnScreenRect();
                framingHelperOverlayView.setFramingRect(dstRect);

                if (zebraOverlayView.getVisibility() == View.VISIBLE) {
                    zebraOverlayView.updateFrame(decodedFrame, dstRect);
                }

                if (focusPeakingOverlayView.getVisibility() == View.VISIBLE) {
                    focusPeakingOverlayView.updateFrame(decodedFrame, dstRect);
                }
            }

            choreographer.postFrameCallback(this);
        }
    };

    public void shutdown() {
        running = false;
        captureManager.stop();
        mainHandler.post(shutdownTask);
        activity.finish();
    }
}
