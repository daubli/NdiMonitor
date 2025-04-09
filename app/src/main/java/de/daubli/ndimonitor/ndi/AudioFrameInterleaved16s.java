package de.daubli.ndimonitor.ndi;

import java.nio.ByteBuffer;

public class AudioFrameInterleaved16s {

    private int referenceLevel;

    private ByteBuffer data;

    public AudioFrameInterleaved16s() {
    }

    public void setReferenceLevel(int referenceLevel) {
        this.referenceLevel = referenceLevel;
    }

    public int getReferenceLevel() {
        return referenceLevel;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    public ByteBuffer getData() {
        return data;
    }
}
