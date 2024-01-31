package de.daubli.ndimonitor;

import java.nio.ByteBuffer;
import java.util.Optional;

import android.media.*;
import android.view.View;
import de.daubli.ndimonitor.view.NdiVideoView;
import me.walkerknapp.devolay.*;

public class StreamNDIVideoRunner extends Thread {

    private final DevolaySource ndiVideoSource;
    private final NdiVideoView ndiVideoView;
    private final StreamNDIVideoActivity activity;
    private volatile boolean running = true;

    private static final int sampleRate = 48000;
    private static final int channelCount = 2;

    float initialFrameRate = 25;

    public StreamNDIVideoRunner(DevolaySource ndiVideoSource, NdiVideoView ndiVideoView,
            StreamNDIVideoActivity activity) {
        super();
        this.ndiVideoSource = ndiVideoSource;
        this.ndiVideoView = ndiVideoView;
        this.activity = activity;
    }

    @Override
    public void run() {
        DevolayReceiver receiver = new DevolayReceiver();

        receiver.connect(ndiVideoSource);
        // Create initial frames to be used for capturing
        DevolayVideoFrame videoFrame = new DevolayVideoFrame();
        DevolayAudioFrame audioFrame = new DevolayAudioFrame();

        // Setup frame to convert floating-point data to 16s data
        DevolayAudioFrameInterleaved16s interleaved16s = new DevolayAudioFrameInterleaved16s();
        interleaved16s.setReferenceLevel(20); // Recommended level for receiving in NDI docs
        interleaved16s.setData(
                ByteBuffer.allocateDirect((int) (sampleRate / initialFrameRate) * channelCount * Short.BYTES));

        final int minBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioAttributes audioAttributes =
                new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build();
        AudioFormat audioFormat =
                new AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build();
        AudioTrack audioTrack =
                new AudioTrack.Builder().setAudioAttributes(audioAttributes).setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(minBufferSize).setTransferMode(AudioTrack.MODE_STREAM).build();

        DevolayFrameSync frameSync = new DevolayFrameSync(receiver);
        // Attach the frame-synchronizer to ensure that audio is dynamically resampled based on request frequency.
        try {
            boolean isFirstFrame = true;
            audioTrack.play();
            byte[] audioData = new byte[0];

            while (running) {
                // Capture audio samples
                frameSync.captureAudio(audioFrame, sampleRate, channelCount, (int) (sampleRate / initialFrameRate));
                // Convert the given float data to interleaved 16s data
                DevolayUtilities.planarFloatToInterleaved16s(audioFrame, interleaved16s);
                // Get the audio data in a byte array, needed to write to a SourceDataLine
                int size = audioFrame.getSamples() * Short.BYTES * audioFrame.getChannels();

                //prevent reallocation of the array
                if (audioData.length != size) {
                    audioData = new byte[size];
                }

                interleaved16s.getData().get(audioData);
                audioTrack.write(audioData, 0, size);

                // Capture a video frame
                if (frameSync.captureVideo(videoFrame)) { // Only returns true if a video frame was returned
                    activity.runOnUiThread(() -> Optional.ofNullable(videoFrame)
                            .ifPresent(frame -> ndiVideoView.setCurrentFrame(videoFrame)));

                    if (isFirstFrame) {
                        initialFrameRate = (float) videoFrame.getFrameRateN() / (float) videoFrame.getFrameRateD();
                        interleaved16s.setData(
                                ByteBuffer.allocateDirect(
                                        (int) (sampleRate / initialFrameRate) * channelCount * Short.BYTES));
                        activity.runOnUiThread(() -> ndiVideoView.setVisibility(View.VISIBLE));
                    }
                    isFirstFrame = false;
                }

                //run with max 50Hz. In case the frame rate is lower - run with the double of the frame rate
                Thread.sleep(Math.round(1000 / (Math.max(initialFrameRate * 2, 50))));
            }
        } catch (InterruptedException interruptedException) {
            // Preserve evidence that the thread was interrupted
            Thread.currentThread().interrupt();
        }
        // Close the frames
        audioTrack.stop();
        videoFrame.close();
        audioFrame.close();
        frameSync.close();
        receiver.close();
        activity.finish();
    }

    public void shutdown() {
        this.running = false;
    }
}
