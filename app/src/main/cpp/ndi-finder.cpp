// Based on code from https://github.com/WalkerKnapp/devolay (Apache 2.0).

#include "ndi-wrapper.h"

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_de_daubli_ndimonitor_ndi_NdiFinder_findCreate(JNIEnv *env, jclass clazz,
                                                  jboolean jShowLocalSources,
                                                  jstring jGroups,
                                                  jstring jExtraIps) {
        auto *NDI_find_create = new NDIlib_find_create_t();
        NDI_find_create->show_local_sources = jShowLocalSources;

        auto *isCopy = new jboolean();
        *isCopy = JNI_TRUE;
        if(jGroups != nullptr) {
            const char *groups = env->GetStringUTFChars(jGroups, isCopy);
            NDI_find_create->p_groups = groups;
        }
        if(jExtraIps != nullptr) {
            const char *extraIps = env->GetStringUTFChars(jExtraIps, isCopy);
            NDI_find_create->p_extra_ips = extraIps;
        }
        delete isCopy;

        auto ret = NDIlib_find_create_v2(NDI_find_create);
        delete NDI_find_create;

        return (jlong) ret;
    }

    JNIEXPORT jlongArray JNICALL
    Java_de_daubli_ndimonitor_ndi_NdiFinder_findGetCurrentSources(JNIEnv *env, jclass clazz, jlong finderPtr) {
        uint32_t no_sources = 0;
        auto instance = reinterpret_cast<NDIlib_find_instance_t>(finderPtr);

        const NDIlib_source_t* p_sources = NDIlib_find_get_current_sources(instance, &no_sources);

        if (p_sources == nullptr || no_sources == 0) {
            // Log info for debugging
            // __android_log_print(ANDROID_LOG_INFO, "NDI", "No NDI sources found or p_sources is null");
            return env->NewLongArray(0);
        }

        auto ret = env->NewLongArray(no_sources);
        for(uint32_t i = 0; i < no_sources; i++) {
            const NDIlib_source_t *source = &p_sources[i];
            const auto pSource = (const jlong) source;
            env->SetLongArrayRegion(ret, i, 1, &pSource);
        }
        return ret;
    }

    JNIEXPORT void JNICALL
    Java_de_daubli_ndimonitor_ndi_NdiFinder_findDestroy(JNIEnv *env, jclass jClazz, jlong pFind) {
        NDIlib_find_destroy(reinterpret_cast<NDIlib_find_instance_t>(pFind));
    }

    JNIEXPORT jboolean JNICALL
    Java_de_daubli_ndimonitor_ndi_NdiFinder_findWaitForSources(JNIEnv *env, jclass jClazz, jlong pFind, jint jTimeout) {
        return NDIlib_find_wait_for_sources(reinterpret_cast<NDIlib_find_instance_t>(pFind), jTimeout);
    }

}