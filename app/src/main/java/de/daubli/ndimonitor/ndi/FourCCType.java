// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

package de.daubli.ndimonitor.ndi;

public enum FourCCType {
    UYVY('U', 'Y', 'V', 'Y'), BGRA('B', 'G', 'R', 'A'), RGBA('R', 'G', 'B', 'A');

    public int id;

    FourCCType(char c1, char c2, char c3, char c4) {
        id = (c1 & 0xFF) | ((c2 & 0xFF) << 8) | ((c3 & 0xFF) << 16) | ((c4 & 0xFF) << 24);
    }

    public static FourCCType valueOf(int id) {
        if (id == UYVY.id) {
            return UYVY;
        } else if (id == BGRA.id) {
            return BGRA;
        } else {
            return null;
        }
    }
}
