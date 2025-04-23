// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

package de.daubli.ndimonitor.ndi;

public class NdiFinder {
    private final long instancePointer;

    private NdiSource[] previouslyQueriedSources;

    public NdiFinder(boolean showLocalSources, String groups, String extraIps) {
        this.instancePointer = findCreate(showLocalSources, groups, extraIps);
    }

    public boolean waitForSources(int timeout) {
        return findWaitForSources(instancePointer, timeout);
    }

    public synchronized NdiSource[] getCurrentSources() {
        closePreviouslyQueriedSources();

        long[] currentSources = findGetCurrentSources(instancePointer);
        NdiSource[] currentNdiSources = new NdiSource[currentSources.length];
        for (int i = 0; i < currentSources.length; i++) {
            currentNdiSources[i] = new NdiSource(currentSources[i]);
        }

        this.previouslyQueriedSources = currentNdiSources;
        return currentNdiSources;
    }

    private void closePreviouslyQueriedSources() {
        if (previouslyQueriedSources != null) {
            for (NdiSource prev : previouslyQueriedSources) {
                prev.close();
            }
        }
    }

    public void close() {
        closePreviouslyQueriedSources();
        findDestroy(instancePointer);
    }

    private native void findDestroy(long finderPtr);

    private native boolean findWaitForSources(long finderPtr, int timeout);

    private native long findCreate(boolean showLocalSources, String groups, String extraIps);

    private native long[] findGetCurrentSources(long finderPtr);
}
