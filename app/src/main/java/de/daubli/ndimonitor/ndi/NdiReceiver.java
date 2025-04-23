// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

package de.daubli.ndimonitor.ndi;

public class NdiReceiver {
    final long instancePointer;

    public NdiReceiver() {
        this.instancePointer = receiveCreateDefaultSettings();
    }

    public void connect(NdiSource ndiSource) {
        receiveConnect(instancePointer, ndiSource == null ? 0L : ndiSource.instancePointer);
    }

    public void close() {
        receiveDestroy(instancePointer);
    }

    private static native long receiveCreateDefaultSettings();

    private static native void receiveConnect(long instancePointer, long pSource);

    private static native void receiveDestroy(long instancePointer);


}
