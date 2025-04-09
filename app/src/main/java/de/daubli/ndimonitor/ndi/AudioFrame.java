package de.daubli.ndimonitor.ndi;

public class AudioFrame {

    private int samples;
    private int channels;

    public AudioFrame() {
    }

    public AudioFrame(int samples, int channels) {
        this.samples = samples;
        this.channels = channels;
    }

    public int getSamples() {
        return 0;
    }

    public int getChannels() {
        return 0;
    }

    public void close() {
        
    }
}
