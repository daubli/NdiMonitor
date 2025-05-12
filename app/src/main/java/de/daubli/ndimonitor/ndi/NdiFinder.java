// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

package de.daubli.ndimonitor.ndi;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NdiFinder {
    private static Logger LOG = Logger.getLogger(NdiFinder.class.getSimpleName());
    private long instancePointer; // no longer final
    private NdiSource[] previouslyQueriedSources;

    public NdiFinder(boolean showLocalSources, String groups, String extraIps) {
        this.instancePointer = findCreate(showLocalSources, groups, extraIps);
    }

    public synchronized boolean waitForSources(int timeout) {
        checkValid();
        return findWaitForSources(instancePointer, timeout);
    }

    public NdiSource getFromQueriedSources(int pos) throws IllegalArgumentException {
        if (previouslyQueriedSources == null || previouslyQueriedSources.length <= pos) {
            throw new IllegalArgumentException("Position is faulty");
        }
        return previouslyQueriedSources[pos];
    }

    public synchronized NdiSource[] getCurrentSources() {
        checkValid();
        closePreviouslyQueriedSources();
        try {
            long[] currentSources = findGetCurrentSources(instancePointer);
            NdiSource[] currentNdiSources = new NdiSource[currentSources.length];
            for (int i = 0; i < currentSources.length; i++) {
                currentNdiSources[i] = new NdiSource(currentSources[i]);
            }
            this.previouslyQueriedSources = currentNdiSources;
            return currentNdiSources;
        } catch (RuntimeException rte) {
            LOG.log(Level.WARNING, rte.getMessage());
            return new NdiSource[]{};
        }
    }

    private void closePreviouslyQueriedSources() {
        if (previouslyQueriedSources != null) {
            for (NdiSource prev : previouslyQueriedSources) {
                prev.close();
            }
            previouslyQueriedSources = null;
        }
    }

    public synchronized void close() {
        closePreviouslyQueriedSources();
        if (instancePointer != 0) {
            findDestroy(instancePointer);
            instancePointer = 0;
        }
    }

    private void checkValid() {
        if (instancePointer == 0) throw new IllegalStateException("NDI finder is closed or destroyed");
    }

    // Native methods
    private native void findDestroy(long finderPtr);

    private native boolean findWaitForSources(long finderPtr, int timeout);

    private native long findCreate(boolean showLocalSources, String groups, String extraIps);

    private native long[] findGetCurrentSources(long finderPtr);
}
