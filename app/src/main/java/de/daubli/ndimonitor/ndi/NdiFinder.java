package de.daubli.ndimonitor.ndi;

public class NdiFinder {
    private final boolean showLocalSources;
    private final String groups;
    private final String extraIps;

    public NdiFinder(boolean showLocalSources, String groups, String extraIps) {
        this.showLocalSources = showLocalSources;
        this.groups = groups;
        this.extraIps = extraIps;
    }

    public boolean waitForSources(int i) {
        return false;
    }

    public Source[] getCurrentSources() {
        return new Source[0];
    }

    public void close() {
    }
}
