// the jni half of 3d sims; handles are only ever passed back from StageNative.kt on the playback thread
#include <jni.h>

#include "engine.h"

using stage::Engine;

namespace {
Engine* engineOf(jlong handle) { return reinterpret_cast<Engine*>(handle); }
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_saurav_pixelmusic_audiolab_stage_StageNative_loadHrtf(JNIEnv* env, jobject, jbyteArray data) {
    jsize size = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) return JNI_FALSE;
    bool ok = stage::loadHrtf(reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(size));
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_saurav_pixelmusic_audiolab_stage_StageNative_create(JNIEnv*, jobject, jint sampleRate, jint channels) {
    if (sampleRate <= 0 || channels < 2 || channels > 8) return 0;
    return reinterpret_cast<jlong>(new Engine(sampleRate, channels));
}

JNIEXPORT void JNICALL
Java_com_saurav_pixelmusic_audiolab_stage_StageNative_setParams(JNIEnv* env, jobject, jlong handle, jfloatArray params) {
    Engine* engine = engineOf(handle);
    if (engine == nullptr) return;
    jsize count = env->GetArrayLength(params);
    if (count != stage::ParamCount) return;
    float values[stage::ParamCount];
    env->GetFloatArrayRegion(params, 0, count, values);
    engine->setParams(values, count);
}

JNIEXPORT jboolean JNICALL
Java_com_saurav_pixelmusic_audiolab_stage_StageNative_process(JNIEnv* env, jobject, jlong handle, jobject input, jint inputOffset,
                                                       jobject output, jint frames) {
    Engine* engine = engineOf(handle);
    auto* in = static_cast<uint8_t*>(env->GetDirectBufferAddress(input));
    auto* out = static_cast<int16_t*>(env->GetDirectBufferAddress(output));
    if (engine == nullptr || in == nullptr || out == nullptr || frames <= 0) return JNI_TRUE;
    return engine->process(reinterpret_cast<const int16_t*>(in + inputOffset), out, frames) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_saurav_pixelmusic_audiolab_stage_StageNative_levels(JNIEnv* env, jobject, jlong handle, jfloatArray out) {
    Engine* engine = engineOf(handle);
    if (engine == nullptr || env->GetArrayLength(out) < stage::KindCount) return;
    float values[stage::KindCount];
    engine->levels(values);
    env->SetFloatArrayRegion(out, 0, stage::KindCount, values);
}

JNIEXPORT void JNICALL
Java_com_saurav_pixelmusic_audiolab_stage_StageNative_clear(JNIEnv*, jobject, jlong handle) {
    if (Engine* engine = engineOf(handle)) engine->clear();
}

JNIEXPORT void JNICALL
Java_com_saurav_pixelmusic_audiolab_stage_StageNative_destroy(JNIEnv*, jobject, jlong handle) {
    delete engineOf(handle);
}

}
