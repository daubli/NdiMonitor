// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

package de.daubli.ndimonitor.ndi;

import java.util.concurrent.atomic.AtomicBoolean;

public class NdiSource {

    private final AtomicBoolean isClosed;

    final long instancePointer;

    public NdiSource(long pointer) {
        this.isClosed = new AtomicBoolean(false);
        this.instancePointer = pointer;
    }

    public String getSourceName() {
        if (isClosed.get()) {
            throw new IllegalStateException("Cannot read source name. Source seems to be closed.");
        }

        return getSourceName(instancePointer);
    }

    public void close() {
        deallocSource(instancePointer);
        isClosed.set(true);
    }

    private static native void deallocSource(long instancePointer);

    private static native String getSourceName(long instancePointer);
}
