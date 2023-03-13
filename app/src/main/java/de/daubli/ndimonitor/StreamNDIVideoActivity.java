package de.daubli.ndimonitor;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Logger;

import com.daubli.ndimonitor.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import de.daubli.ndimonitor.view.NdiVideoView;
import me.walkerknapp.devolay.*;

public class StreamNDIVideoActivity extends AppCompatActivity {

    DevolaySource ndiVideoSource;
    NdiVideoView ndiVideoView;
    FloatingActionButton fab;

    boolean shouldInterrupt = false;
    
    private static final Logger LOG = Logger.getLogger(StreamNDIVideoActivity.class.getSimpleName());

    private static final int sampleRate = 48000;
    private static final int channelCount = 2;

    final float clockSpeed = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ndiVideoSource = MainActivity.getSource();
        setContentView(R.layout.stream_ndi_video_activity);
        ndiVideoView = findViewById(R.id.ndiVideoView);
        fab = findViewById(R.id.regular_fab);
        fab.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();
        fab.setOnClickListener((View v) -> shouldInterrupt = true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(() -> {
            try {
                stream();
            } catch (InterruptedException e) {
                LOG.warning("Stream Interrupted: " + e.getMessage());
                this.finish();
            }
        }).start();
    }

    private void stream() throws InterruptedException {
        DevolayReceiver receiver = new DevolayReceiver();
        receiver.connect(ndiVideoSource);
        // Create initial frames to be used for capturing
        DevolayVideoFrame videoFrame = new DevolayVideoFrame();
        DevolayAudioFrame audioFrame = new DevolayAudioFrame();

        // Setup frame to convert floating-point data to 16s data
        DevolayAudioFrameInterleaved16s interleaved16s = new DevolayAudioFrameInterleaved16s();
        interleaved16s.setReferenceLevel(20); // Recommended level for receiving in NDI docs
        interleaved16s.setData(ByteBuffer.allocateDirect((int) (sampleRate / clockSpeed) * channelCount * Short.BYTES));

        final int minBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize,
                AudioTrack.MODE_STREAM);

        DevolayFrameSync frameSync = new DevolayFrameSync(receiver);
        // Attach the frame-synchronizer to ensure that audio is dynamically resampled based on request frequency.
        try {
            // Run for one minute
            boolean isFirstFrame = true;

            audioTrack.play();

            while (true) {
                if (receiver.getConnectionCount() < 1) {
                    receiver.connect(ndiVideoSource);
                }

                // Capture a video frame
                if (frameSync.captureVideo(videoFrame)) { // Only returns true if a video frame was returned
                    boolean finalIsFirstFrame = isFirstFrame;
                    runOnUiThread(() -> Optional.ofNullable(videoFrame)
                            .ifPresent(frame -> ndiVideoView.setCurrentFrame(videoFrame, finalIsFirstFrame)));

                    if (isFirstFrame) {
                        runOnUiThread(() -> ndiVideoView.setVisibility(View.VISIBLE));
                    }
                    isFirstFrame = false;
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
                    break;
                }
                Thread.sleep((long) (1000 / clockSpeed));
            }
        } finally {
            // Destroy the references to each. Not necessary, but can free up the memory faster than Java's GC by itself
            videoFrame.close();
            audioFrame.close();
            runOnUiThread(() -> {
                ndiVideoView.setVisibility(View.INVISIBLE);
                ndiVideoView.deleteCurrentFrameData();
                // Make sure to close the framesync before the receiver
                frameSync.close();
                receiver.close();

                this.finish();
            });
        }
    }
}
