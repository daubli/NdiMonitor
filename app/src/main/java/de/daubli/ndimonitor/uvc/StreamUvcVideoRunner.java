package de.daubli.ndimonitor.uvc;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import de.daubli.ndimonitor.StreamVideoActivity;
import de.daubli.ndimonitor.StreamVideoRunner;
import de.daubli.ndimonitor.databinding.StreamVideoActivityBinding;
import de.daubli.ndimonitor.decoder.MJpegBitmapBuilder;
import de.daubli.ndimonitor.view.focusassist.FocusPeakingOverlayView;
import de.daubli.ndimonitor.view.framehelper.FramingHelperOverlayView;
import de.daubli.ndimonitor.view.video.bitmap.BitmapVideoView;
import de.daubli.ndimonitor.view.zebra.ZebraOverlayView;

public class StreamUvcVideoRunner extends Thread implements StreamVideoRunner {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final BitmapVideoView videoView;

    private final FramingHelperOverlayView framingHelperOverlayView;

    private final FocusPeakingOverlayView focusPeakingOverlayView;

    private final ZebraOverlayView zebraOverlayView;

    private final StreamVideoActivity activity;

    private final UvcCaptureManager captureManager;

    private final AtomicReference<Bitmap> latestBitmap = new AtomicReference<>();

    private final ExecutorService decodeExecutor = Executors.newSingleThreadExecutor();

    private final AtomicBoolean decodeInProgress = new AtomicBoolean(false);

    private volatile boolean running = true;

    public StreamUvcVideoRunner(UVCSource uvcSource, StreamVideoActivityBinding binding, StreamVideoActivity activity) {
        this.captureManager = new UvcCaptureManager(uvcSource, activity);
        this.videoView = binding.bitmapVideoView;
        this.framingHelperOverlayView = binding.framingHelperOverlayView;
        this.zebraOverlayView = binding.zebraOverlayView;
        this.focusPeakingOverlayView = binding.focusPeakingOverlayView;
        this.activity = activity;
    }

    @Override
    public void run() {
        captureManager.start(data -> {

            // Drop frame if decoder is busy -> when decoder is free, set the flag to true
            if (!decodeInProgress.compareAndSet(false, true)) {
                return;
            }

            ByteBuffer buffer = ByteBuffer.wrap(data.clone());

            decodeExecutor.execute(() -> {
                try {
                    Bitmap bitmap = MJpegBitmapBuilder.builder().withRawData(buffer).build();

                    if (bitmap != null) {
                        Bitmap old = latestBitmap.getAndSet(bitmap);
                        if (old != null) {
                            old.recycle();
                        }
                    }
                } finally {
                    //decoding finished
                    decodeInProgress.set(false);
                }
            });
        });
        mainHandler.post(renderRunnable);
    }

    private final Runnable renderRunnable = new Runnable() {

        @Override
        public void run() {
            if (!running) {
                return;
            }

            Bitmap bitmap = latestBitmap.getAndSet(null);
            if (bitmap != null) {
                videoView.setVisibility(View.VISIBLE);
                videoView.updateFrame(bitmap);
                Rect dstRect = videoView.getLocationOnScreenRect();
                framingHelperOverlayView.setFramingRect(dstRect);

                if (zebraOverlayView.getVisibility() == View.VISIBLE) {
                    zebraOverlayView.updateFrame(bitmap, dstRect);
                }

                if (focusPeakingOverlayView.getVisibility() == View.VISIBLE) {
                    focusPeakingOverlayView.updateFrame(bitmap, dstRect);
                }
            }

            videoView.postOnAnimation(this);
        }
    };

    @Override
    public void shutdown() {
        running = false;
        captureManager.stop();

        mainHandler.removeCallbacks(renderRunnable);
        activity.finish();
    }
}
