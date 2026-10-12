// Minimal JNI bridge to whisper.cpp: load a model, transcribe one short clip, free.
#include <jni.h>
#include <android/log.h>
#include <string>

#include "whisper.h"

#define TAG "WhisperJni"

extern "C" {

JNIEXPORT jlong JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_init(JNIEnv *env, jobject, jstring path) {
    const char *p = env->GetStringUTFChars(path, nullptr);
    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    whisper_context *ctx = whisper_init_from_file_with_params(p, cparams);
    if (ctx == nullptr) __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to load %s", p);
    env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_free(JNIEnv *, jobject, jlong ptr) {
    whisper_free(reinterpret_cast<whisper_context *>(ptr));
}

JNIEXPORT jstring JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_transcribe(
        JNIEnv *env, jobject, jlong ptr, jfloatArray audio, jstring language, jint threads, jint audioCtx) {
    auto *ctx = reinterpret_cast<whisper_context *>(ptr);
    if (ctx == nullptr) return nullptr;

    const char *lang = env->GetStringUTFChars(language, nullptr);
    std::string langCopy(lang);
    env->ReleaseStringUTFChars(language, lang);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = langCopy.c_str();
    params.n_threads = threads;
    params.no_context = true;
    params.single_segment = true;
    params.no_timestamps = true;
    params.suppress_blank = true;
    // Encoder context sized to the clip (default is a full 30 s window): much faster for short phrases.
    params.audio_ctx = audioCtx;

    jsize n = env->GetArrayLength(audio);
    jfloat *data = env->GetFloatArrayElements(audio, nullptr);
    int rc = whisper_full(ctx, params, data, n);
    env->ReleaseFloatArrayElements(audio, data, JNI_ABORT);
    if (rc != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "whisper_full failed: %d", rc);
        return nullptr;
    }

    std::string text;
    int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; ++i) text += whisper_full_get_segment_text(ctx, i);
    return env->NewStringUTF(text.c_str());
}

}  // extern "C"
