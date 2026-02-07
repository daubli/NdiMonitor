// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

#include "ndi-wrapper.h"

extern "C" {

    JNIEXPORT jlong JNICALL
    Java_de_daubli_ndimonitor_ndi_NdiReceiver_receiveCreateDefaultSettings(
        JNIEnv *env, jclass jClazz) {
        NDIlib_recv_create_v3_t recv_create_struct;
        std::memset(&recv_create_struct, 0, sizeof(recv_create_struct));

        recv_create_struct.color_format = NDIlib_recv_color_format_fastest;
        recv_create_struct.bandwidth    = NDIlib_recv_bandwidth_highest;
        recv_create_struct.allow_video_fields = true;
        recv_create_struct.source_to_connect_to = nullptr;

        NDIlib_recv_instance_t receiver = NDIlib_recv_create_v3(&recv_create_struct);

        return reinterpret_cast<jlong>(receiver);
}

    JNIEXPORT void JNICALL
    Java_de_daubli_ndimonitor_ndi_NdiReceiver_receiveConnect(JNIEnv *env, jclass jClazz, jlong pReceiver, jlong pSource) {
        NDIlib_recv_connect(reinterpret_cast<NDIlib_recv_instance_t>(pReceiver), reinterpret_cast<NDIlib_source_t *>(pSource));
    }

    JNIEXPORT void JNICALL
    Java_de_daubli_ndimonitor_ndi_NdiReceiver_receiveDestroy(JNIEnv *env, jclass jClazz, jlong pReceiver) {
        NDIlib_recv_destroy(reinterpret_cast<NDIlib_recv_instance_t>(pReceiver));
    }

}