// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

package de.daubli.ndimonitor.ndi;

abstract class NdiFrameCleaner {
    abstract void freeVideo(NdiVideoFrame videoFrame);

    abstract void freeAudio(NdiAudioFrame ndiAudioFrame);
}
