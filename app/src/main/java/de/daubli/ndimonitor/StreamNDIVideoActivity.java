package de.daubli.ndimonitor;

import java.nio.ByteBuffer;
import java.util.Optional;

import com.daubli.ndimonitor.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.media.*;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import de.daubli.ndimonitor.view.NdiVideoView;
import me.walkerknapp.devolay.*;

public class StreamNDIVideoActivity extends AppCompatActivity {

    DevolaySource ndiVideoSource;
    NdiVideoView ndiVideoView;
    FloatingActionButton closeButton;

    boolean shouldInterrupt = false;

    private static final int sampleRate = 48000;
    private static final int channelCount = 2;

    final float clockSpeed = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ndiVideoSource = MainActivity.getSource();
        setContentView(R.layout.stream_ndi_video_activity);
        ndiVideoView = findViewById(R.id.ndiVideoView);
        closeButton = findViewById(R.id.regular_fab);
        closeButton.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();
        closeButton.setOnClickListener((View v) -> shouldInterrupt = true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(this::stream).start();
    }

    private void stream() {
        DevolayReceiver receiver = new DevolayReceiver();

        receiver.connect(ndiVideoSource);
        // Create initial frames to be used for capturing
        DevolayVideoFrame videoFrame = new DevolayVideoFrame();
        DevolayAudioFrame audioFrame = new DevolayAudioFrame();

        // Setup frame to convert floating-point data to 16s data
        DevolayAudioFrameInterleaved16s interleaved16s = new DevolayAudioFrameInterleaved16s();
        interleaved16s.setReferenceLevel(20); // Recommended level for receiving in NDI docs
        interleaved16s.setData(
                ByteBuffer.allocateDirect((int) (sampleRate / clockSpeed) * channelCount * Short.BYTES));

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
            // Run for one minute
            boolean isFirstFrame = true;

            audioTrack.play();

            while (true) {
                long startTimeStamp = System.currentTimeMillis();

                if (receiver.getConnectionCount() < 1) {
                    receiver.connect(ndiVideoSource);
                }

                // Capture audio samples
                frameSync.captureAudio(audioFrame, sampleRate, channelCount, (int) (sampleRate / clockSpeed));
                // Convert the given float data to interleaved 16s data
                DevolayUtilities.planarFloatToInterleaved16s(audioFrame, interleaved16s);
                // Get the audio data in a byte array, needed to write to a SourceDataLine
                int size = audioFrame.getSamples() * Short.BYTES * audioFrame.getChannels();
                byte[] audioData = new byte[size];
                interleaved16s.getData().get(audioData);

                audioTrack.write(audioData, 0, size);

                if (shouldInterrupt) {
                    audioTrack.stop();
                    throw new InterruptedException();
                }

                // Capture a video frame
                if (frameSync.captureVideo(videoFrame)) { // Only returns true if a video frame was returned
                    runOnUiThread(() -> Optional.ofNullable(videoFrame)
                            .ifPresent(frame -> ndiVideoView.setCurrentFrame(videoFrame)));

                    if (isFirstFrame) {
                        runOnUiThread(() -> ndiVideoView.setVisibility(View.VISIBLE));
                    }
                    isFirstFrame = false;
                }

                long loopDuration = System.currentTimeMillis() - startTimeStamp;

                if (loopDuration < (1000 / clockSpeed)) {
                    Thread.sleep((long) (1000 / clockSpeed) - loopDuration);
                }
            }
        } catch (InterruptedException interruptedException) {
            videoFrame.close();
            audioFrame.close();

            frameSync.close();
            receiver.close();

            runOnUiThread(() -> {
                ndiVideoView.setVisibility(View.INVISIBLE);
                ndiVideoView.deleteCurrentFrameData();
                // Make sure to close the framesync before the receiver
                this.finish();
            });
            Thread.currentThread().interrupt();
        }
    }
}
