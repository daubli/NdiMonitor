// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

#include "ndi-wrapper.h"

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_de_daubli_ndimonitor_ndi_NdiReceiver_receiveCreateDefaultSettings(JNIEnv *env, jclass jClazz) {
        auto *recv_create_struct = new NDIlib_recv_create_v3_t();
        auto *ret = NDIlib_recv_create_v3(recv_create_struct);
        delete recv_create_struct;
        return reinterpret_cast<jlong>(ret);
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