package de.daubli.ndimonitor.ndi;

public class FrameSync {
    private final NdiReceiver receiver;

    public FrameSync(NdiReceiver receiver) {
        this.receiver = receiver;
    }

    public void captureAudio(AudioFrame audioFrame, int sampleRate, int channelCount, int sampleCount) {

    }

    public boolean captureVideo(VideoFrame videoFrame) {
        return false;
    }

    public void close() {
        
    }
}
