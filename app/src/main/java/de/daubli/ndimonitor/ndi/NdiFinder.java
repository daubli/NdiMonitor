// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

package de.daubli.ndimonitor.ndi;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NdiFinder {
    private static final Logger LOG = Logger.getLogger(NdiFinder.class.getSimpleName());
    private final boolean showLocalSources;
    private final String groups;
    private final String extraIps;
    private long instancePointer; // no longer final
    private NdiSource[] previouslyQueriedSources;

    public NdiFinder(boolean showLocalSources, String groups, String extraIps) {
        this.showLocalSources = showLocalSources;
        this.groups = groups;
        this.extraIps = extraIps;
        initNewFinderInstance();
    }

    public void initNewFinderInstance() {
        this.instancePointer = findCreate(showLocalSources, groups, extraIps);
    }

    public synchronized void waitForSources(int timeout) {
        if (isInstanceUnhealthy()) {
            initNewFinderInstance();
        }
        findWaitForSources(instancePointer, timeout);
    }

    public NdiSource getFromQueriedSources(String name) throws IllegalArgumentException {
        return Arrays.stream(previouslyQueriedSources).filter(ndiSource -> name.equals(ndiSource.getSourceName())).findFirst().orElseThrow(() -> new IllegalArgumentException("Position is faulty"));
    }

    public synchronized NdiSource[] getCurrentSources() {
        if (isInstanceUnhealthy()) {
            initNewFinderInstance();
        }

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

    private boolean isInstanceUnhealthy() {
        return instancePointer == 0;
    }

    // Native methods
    private native void findDestroy(long finderPtr);

    private native boolean findWaitForSources(long finderPtr, int timeout);

    private native long findCreate(boolean showLocalSources, String groups, String extraIps);

    private native long[] findGetCurrentSources(long finderPtr);
}
