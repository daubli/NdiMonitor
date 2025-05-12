package de.daubli.ndimonitor;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.*;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import de.daubli.ndimonitor.audio.AudioUtils;
import de.daubli.ndimonitor.ndi.*;
import de.daubli.ndimonitor.decoder.NdiFrameDecoder;
import de.daubli.ndimonitor.view.focusassist.FocusPeakingOverlayView;
import de.daubli.ndimonitor.view.FramingHelperOverlayView;
import de.daubli.ndimonitor.view.VideoView;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class StreamNDIVideoRunner extends Thread {
    private static final int sampleRate = 48000;
    private static final int channelCount = 2;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final NdiSource ndiVideoNdiSource;
    private final VideoView videoView;

    private final FramingHelperOverlayView framingHelperOverlayView;

    private final FocusPeakingOverlayView focusPeakingOverlayView;
    private final StreamNDIVideoActivity activity;
    private Choreographer choreographer;
    private NdiFrameSync frameSync;
    private NdiReceiver receiver;
    private NdiVideoFrame ndiVideoFrame;

    private NdiAudioFrame ndiAudioFrame;

    private Thread audioThread;
    //
    private AudioTrack audioTrack;
    private float frameRate = 25f;
    private boolean firstFramePending = true;

    private boolean firstAudioFrameReceived = false;
    private volatile boolean running = true;

    private final Runnable shutdownTask = () -> {
        if (choreographer != null) choreographer.removeFrameCallback(this.frameCallback);
    };

    public StreamNDIVideoRunner(NdiSource ndiVideoNdiSource, VideoView videoView,
                                FramingHelperOverlayView framingHelperOverlayView,
                                FocusPeakingOverlayView focusPeakingOverlayView,
                                StreamNDIVideoActivity activity) {
        super();
        this.ndiVideoNdiSource = ndiVideoNdiSource;
        this.framingHelperOverlayView = framingHelperOverlayView;
        this.videoView = videoView;
        this.focusPeakingOverlayView = focusPeakingOverlayView;
        this.activity = activity;
    }

    @Override
    public void run() {
        receiver = new NdiReceiver();
        receiver.connect(ndiVideoNdiSource);

        frameSync = new NdiFrameSync(receiver);
        ndiVideoFrame = new NdiVideoFrame();
        ndiAudioFrame = new NdiAudioFrame();

        // Init AudioTrack
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(minBufferSize * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        audioTrack.play();

        mainHandler.post(() -> {
            choreographer = Choreographer.getInstance();
            choreographer.postFrameCallback(frameCallback);
        });

        audioThread = new Thread(() -> {
            final int bytesPerSample = 2; // 16-bit PCM
            long nextPlayTime = System.nanoTime();

            while (running) {
                frameSync.captureAudio(ndiAudioFrame, sampleRate, channelCount, (int) (sampleRate / frameRate));
                ByteBuffer audioBuffer = AudioUtils.convertPlanarFloatToInterleavedPCM16(
                        ndiAudioFrame.getData(), ndiAudioFrame.getChannels());

                int sampleCount = audioBuffer.remaining() / (bytesPerSample * channelCount);

                if (sampleCount > 0) {
                    this.firstAudioFrameReceived = true;
                }

                long chunkDurationNanos = (sampleCount * 1_000_000_000L) / sampleRate;

                byte[] audioData = new byte[audioBuffer.remaining()];
                audioBuffer.get(audioData);
                audioTrack.write(audioData, 0, audioData.length);

                // Update next play time
                nextPlayTime += chunkDurationNanos;

                long now = System.nanoTime();
                long sleepTime = nextPlayTime - now;

                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime / 1_000_000L, (int) (sleepTime % 1_000_000L));
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    nextPlayTime = now;
                }
            }

            stopPlayback();
        });
        audioThread.start();
    }

    private void stopPlayback() {
        audioTrack.stop();
        audioTrack.release();
        frameSync.close();
        receiver.close();

        mainHandler.post(shutdownTask);
        activity.finish();
    }

    public void shutdown() {
        running = false;
    }

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!running) return;

            if (firstAudioFrameReceived) {
                videoView.setVisibility(View.VISIBLE);
                firstFramePending = false;
            }

            if (frameSync.captureVideo(ndiVideoFrame)) {
                float newFrameRate = (float) ndiVideoFrame.getFrameRateN() / ndiVideoFrame.getFrameRateD();

                if (firstFramePending) {
                    videoView.setVisibility(View.VISIBLE);
                    firstFramePending = false;
                }

                if (frameRate != newFrameRate) {
                    frameRate = newFrameRate;
                }

                Bitmap decodedFrame = NdiFrameDecoder.decode(ndiVideoFrame, videoView.getHeight());
                videoView.updateFrame(decodedFrame);
                Rect dstRect = videoView.getLocationOnScreenRect();
                framingHelperOverlayView.setFramingRect(dstRect);
                if (decodedFrame == null) {
                    return;
                }

                if (focusPeakingOverlayView.getVisibility() == View.VISIBLE) {
                    focusPeakingOverlayView.updateFrame(decodedFrame, dstRect);
                }
            }

            choreographer.postFrameCallback(this);
        }
    };
}

