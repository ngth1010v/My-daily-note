// Minimal JNI bridge: whisper.cpp (load a model, transcribe one short clip, free) and RNNoise (denoise a clip).
#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <string>
#include <vector>

#include "rnnoise.h"
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

// Returns one "<prob>\t<word>\n" line per recognized word; prob = mean over the word's tokens (0..1).
// Not the weakest token: a Vietnamese word is often several byte-level tokens and one of them nearly
// always looked unsure, so almost every Vietnamese clip went through the recheck.
// beamSize <= 1 = greedy sampling (best_of 5, useful with temperature > 0). vadPath / prompt empty = unused.
JNIEXPORT jstring JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_transcribe(
        JNIEnv *env, jobject, jlong ptr, jfloatArray audio, jstring language, jint threads,
        jint beamSize, jfloat temperature, jstring vadPath, jstring prompt) {
    auto *ctx = reinterpret_cast<whisper_context *>(ptr);
    if (ctx == nullptr) return nullptr;

    const char *lang = env->GetStringUTFChars(language, nullptr);
    std::string langCopy(lang);
    env->ReleaseStringUTFChars(language, lang);
    const char *vad = env->GetStringUTFChars(vadPath, nullptr);
    std::string vadCopy(vad);
    env->ReleaseStringUTFChars(vadPath, vad);
    const char *pr = env->GetStringUTFChars(prompt, nullptr);
    std::string promptCopy(pr);
    env->ReleaseStringUTFChars(prompt, pr);

    whisper_full_params params = whisper_full_default_params(
            beamSize > 1 ? WHISPER_SAMPLING_BEAM_SEARCH : WHISPER_SAMPLING_GREEDY);
    params.beam_search.beam_size = beamSize;
    params.greedy.best_of = 5;
    params.temperature = temperature;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = langCopy.c_str();
    params.n_threads = threads;
    params.no_context = true;
    params.suppress_blank = true;
    params.suppress_nst = true;
    if (!promptCopy.empty()) params.initial_prompt = promptCopy.c_str();
    if (!vadCopy.empty()) {
        params.vad = true;
        params.vad_model_path = vadCopy.c_str();
    }

    jsize n = env->GetArrayLength(audio);
    jfloat *data = env->GetFloatArrayElements(audio, nullptr);
    int rc = whisper_full(ctx, params, data, n);
    env->ReleaseFloatArrayElements(audio, data, JNI_ABORT);
    if (rc != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "whisper_full failed: %d", rc);
        return nullptr;
    }

    // A token starting with a space opens a new word; the others continue it.
    std::string out, word;
    float probSum = 0.f;
    int probCount = 0;
    auto flush = [&]() {
        if (!word.empty() && probCount > 0) out += std::to_string(probSum / probCount) + "\t" + word + "\n";
        word.clear();
        probSum = 0.f;
        probCount = 0;
    };
    const whisper_token eot = whisper_token_eot(ctx);
    int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; ++i) {
        int tokens = whisper_full_n_tokens(ctx, i);
        for (int t = 0; t < tokens; ++t) {
            whisper_token_data d = whisper_full_get_token_data(ctx, i, t);
            if (d.id >= eot) continue;  // special and timestamp tokens
            const char *text = whisper_full_get_token_text(ctx, i, t);
            if (text == nullptr || text[0] == '\0') continue;
            if (text[0] == ' ') flush();
            for (const char *c = text; *c; ++c) if (*c != ' ' && *c != '\n' && *c != '\t') word += *c;
            probSum += d.p;
            ++probCount;
        }
        flush();
    }
    return env->NewStringUTF(out.c_str());
}

// Returns whichever of the candidate language codes ("en,vi") Whisper finds more likely for the clip.
JNIEXPORT jstring JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_detectLanguage(
        JNIEnv *env, jobject, jlong ptr, jfloatArray audio, jint threads, jstring candidates) {
    auto *ctx = reinterpret_cast<whisper_context *>(ptr);
    const char *c = env->GetStringUTFChars(candidates, nullptr);
    std::string list(c);
    env->ReleaseStringUTFChars(candidates, c);
    std::string first = list.substr(0, list.find(','));
    if (ctx == nullptr) return env->NewStringUTF(first.c_str());

    jsize n = env->GetArrayLength(audio);
    jfloat *data = env->GetFloatArrayElements(audio, nullptr);
    int rc = whisper_pcm_to_mel(ctx, data, n, threads);
    env->ReleaseFloatArrayElements(audio, data, JNI_ABORT);
    if (rc != 0) return env->NewStringUTF(first.c_str());
    std::vector<float> probs(whisper_lang_max_id() + 1, 0.f);
    if (whisper_lang_auto_detect(ctx, 0, threads, probs.data()) < 0) return env->NewStringUTF(first.c_str());

    std::string best = first;
    float bestP = -1.f;
    size_t start = 0;
    while (start <= list.size()) {
        size_t end = list.find(',', start);
        if (end == std::string::npos) end = list.size();
        std::string code = list.substr(start, end - start);
        int id = whisper_lang_id(code.c_str());
        if (id >= 0 && probs[id] > bestP) { bestP = probs[id]; best = code; }
        start = end + 1;
    }
    return env->NewStringUTF(best.c_str());
}

// Streaming Silero VAD for auto-stop: one context per mic session, fed chunk by chunk.
JNIEXPORT jlong JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_vadInit(JNIEnv *env, jobject, jstring path) {
    const char *p = env->GetStringUTFChars(path, nullptr);
    whisper_vad_context_params vparams = whisper_vad_default_context_params();
    vparams.n_threads = 1;
    vparams.use_gpu = false;
    whisper_vad_context *vctx = whisper_vad_init_from_file_with_params(p, vparams);
    if (vctx == nullptr) __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to load VAD %s", p);
    env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(vctx);
}

JNIEXPORT void JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_vadFree(JNIEnv *, jobject, jlong ptr) {
    whisper_vad_free(reinterpret_cast<whisper_vad_context *>(ptr));
}

JNIEXPORT void JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_vadReset(JNIEnv *, jobject, jlong ptr) {
    whisper_vad_reset_state(reinterpret_cast<whisper_vad_context *>(ptr));
}

// Highest speech probability over the samples (a whole number of 512-sample windows); -1 on error.
JNIEXPORT jfloat JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_vadMaxProb(
        JNIEnv *env, jobject, jlong ptr, jfloatArray audio) {
    auto *vctx = reinterpret_cast<whisper_vad_context *>(ptr);
    jsize n = env->GetArrayLength(audio);
    jfloat *data = env->GetFloatArrayElements(audio, nullptr);
    bool ok = whisper_vad_detect_speech_no_reset(vctx, data, n);
    env->ReleaseFloatArrayElements(audio, data, JNI_ABORT);
    if (!ok) return -1.f;
    float best = 0.f;
    const float *probs = whisper_vad_probs(vctx);
    for (int i = 0; i < whisper_vad_n_probs(vctx); ++i) best = std::max(best, probs[i]);
    return best;
}

// Denoises 16 kHz mono PCM with RNNoise (48 kHz internally, frames of 10 ms). Same length out.
JNIEXPORT jshortArray JNICALL
Java_io_github_ngth1010v_mydailynote_speech_whisper_WhisperNative_denoise(JNIEnv *env, jobject, jshortArray pcm) {
    jsize n = env->GetArrayLength(pcm);
    std::vector<jshort> in(n);
    env->GetShortArrayRegion(pcm, 0, n, in.data());

    const int frame = rnnoise_get_frame_size();  // 480 samples at 48 kHz
    size_t up = static_cast<size_t>(n) * 3;
    size_t padded = (up + frame - 1) / frame * frame;
    std::vector<float> x(padded, 0.f);
    for (jsize i = 0; i < n; ++i) {  // linear 3x upsampling
        float a = in[i], b = i + 1 < n ? in[i + 1] : in[i];
        x[i * 3] = a;
        x[i * 3 + 1] = a + (b - a) / 3.f;
        x[i * 3 + 2] = a + (b - a) * 2.f / 3.f;
    }
    DenoiseState *st = rnnoise_create(nullptr);
    std::vector<float> y(padded);
    for (size_t off = 0; off < padded; off += frame) rnnoise_process_frame(st, y.data() + off, x.data() + off);
    rnnoise_destroy(st);

    std::vector<jshort> out(n);
    for (jsize i = 0; i < n; ++i) {  // average of 3 = crude low-pass + 3x downsampling
        float v = (y[i * 3] + y[i * 3 + 1] + y[i * 3 + 2]) / 3.f;
        out[i] = static_cast<jshort>(std::lround(std::max(-32768.f, std::min(32767.f, v))));
    }
    jshortArray result = env->NewShortArray(n);
    env->SetShortArrayRegion(result, 0, n, out.data());
    return result;
}

}  // extern "C"
