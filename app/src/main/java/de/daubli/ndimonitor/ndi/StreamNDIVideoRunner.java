package de.daubli.ndimonitor.ndi;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import android.media.*;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import de.daubli.ndimonitor.StreamVideoActivity;
import de.daubli.ndimonitor.StreamVideoRunner;
import de.daubli.ndimonitor.audio.AudioRingBuffer;
import de.daubli.ndimonitor.audio.AudioUtils;
import de.daubli.ndimonitor.databinding.StreamVideoActivityBinding;
import de.daubli.ndimonitor.view.focusassist.FocusPeakingOverlayView;
import de.daubli.ndimonitor.view.framehelper.FramingHelperOverlayView;
import de.daubli.ndimonitor.view.video.opengl.OpenGLVideoView;
import de.daubli.ndimonitor.view.zebra.ZebraOverlayView;

public class StreamNDIVideoRunner extends Thread implements StreamVideoRunner {

    private static final int SAMPLE_RATE = 48_000;

    private static final int CHANNEL_COUNT = 2;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final NdiSource ndiSource;

    private final StreamVideoActivity activity;

    private final OpenGLVideoView videoView;

    private final FramingHelperOverlayView framingHelperOverlayView;

    private final FocusPeakingOverlayView focusPeakingOverlayView;

    private final ZebraOverlayView zebraOverlayView;

    private NdiReceiver receiver;

    private NdiFrameSync frameSync;

    private AudioTrack audioTrack;

    private Choreographer choreographer;

    private volatile boolean running = true;

    private volatile boolean firstAudioFrameReceived = false;

    private boolean firstFramePending = true;

    private Thread audioThread;

    private final AtomicReference<NdiVideoFrame> renderFrame = new AtomicReference<>();

    private NdiVideoFrame captureFrame;

    private final AudioRingBuffer ringBuffer = new AudioRingBuffer(20, 4000 * 2 * CHANNEL_COUNT);

    public StreamNDIVideoRunner(NdiSource ndiSource, StreamVideoActivityBinding binding, StreamVideoActivity activity) {
        this.ndiSource = ndiSource;
        this.activity = activity;
        this.videoView = binding.openGLVideoView;
        this.framingHelperOverlayView = binding.framingHelperOverlayView;
        this.focusPeakingOverlayView = binding.focusPeakingOverlayView;
        this.zebraOverlayView = binding.zebraOverlayView;
    }

    @Override
    public void run() {
        receiver = new NdiReceiver();
        receiver.connect(ndiSource);

        frameSync = new NdiFrameSync(receiver);

        captureFrame = new NdiVideoFrame();
        renderFrame.set(new NdiVideoFrame());

        initAudio();
        startAudioCaptureThread();
        startAudioThread();
        startVideoCaptureThread();

        mainHandler.post(() -> {
            choreographer = Choreographer.getInstance();
            choreographer.postFrameCallback(frameCallback);
        });
    }

    @Override
    public void shutdown() {
        running = false;

        mainHandler.post(() -> {
            if (choreographer != null) {
                choreographer.removeFrameCallback(frameCallback);
            }
        });

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }

        if (frameSync != null) {
            frameSync.close();
        }
        if (receiver != null) {
            receiver.close();
        }

        activity.finish();
    }

    private void initAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack.Builder().setAudioAttributes(
                        new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build()).setAudioFormat(
                        new AudioFormat.Builder().setSampleRate(SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()).setBufferSizeInBytes(minBufferSize * 8)
                .setTransferMode(AudioTrack.MODE_STREAM).build();

        audioTrack.play();
    }

    private void startAudioThread() {
        audioThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            while (running) {
                byte[] frameData = ringBuffer.read();
                if (frameData != null) {
                    audioTrack.write(frameData, 0, frameData.length);
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "NDI-AudioTrack");

        audioThread.start();
    }

    private void startAudioCaptureThread() {
        new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            NdiAudioFrame audioFrame = new NdiAudioFrame();

            while (running) {
                frameSync.captureAudio(audioFrame, SAMPLE_RATE, CHANNEL_COUNT, 4000);

                ByteBuffer pcm = AudioUtils.convertPlanarFloatToInterleavedPCM16(audioFrame.getData(),
                        audioFrame.getChannels());

                if (pcm.remaining() > 0) {
                    firstAudioFrameReceived = true;
                    byte[] data = new byte[pcm.remaining()];
                    pcm.get(data);

                    if (AudioUtils.isSilentFast(data)) {
                        continue;
                    }
                    ringBuffer.write(data);
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "NDI-Audio-Capture").start();
    }

    private void startVideoCaptureThread() {
        new Thread(() -> {

            while (running) {

                if (frameSync.captureVideo(captureFrame)) {
                    NdiVideoFrame old = renderFrame.getAndSet(captureFrame);
                    captureFrame = old;
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "NDI-Video-Capture").start();
    }

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {

        @Override
        public void doFrame(long frameTimeNanos) {
            if (!running) {
                return;
            }

            NdiVideoFrame frame = renderFrame.get();
            if (frame != null) {

                if (firstFramePending && firstAudioFrameReceived) {
                    videoView.setVisibility(View.VISIBLE);
                    firstFramePending = false;
                }

                videoView.updateFrame(frame.getData(), frame.getXResolution(), frame.getYResolution(),
                        frame.getFourCCType());

                framingHelperOverlayView.setFramingRect(videoView.getVideoRect());
            }

            choreographer.postFrameCallback(this);
        }
    };
}
