#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <cstdio>
#include <cstring>
#include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ---- Audio Loading (WAV only, 16-bit PCM) ----
static std::vector<float> load_wav_16k_mono(const char* path) {
    FILE* f = fopen(path, "rb");
    if (!f) { LOGE("fopen failed: %s", path); return {}; }
    
    char header[44];
    if (fread(header, 1, 44, f) != 44) { fclose(f); return {}; }
    
    int sample_rate = *reinterpret_cast<int*>(header + 24);
    short num_channels = *reinterpret_cast<short*>(header + 22);
    short bits_per_sample = *reinterpret_cast<short*>(header + 34);
    int data_size = *reinterpret_cast<int*>(header + 40);
    
    if (bits_per_sample != 16) { LOGE("Not 16-bit PCM"); fclose(f); return {}; }
    
    std::vector<short> pcm(data_size / 2);
    fread(pcm.data(), 2, pcm.size(), f);
    fclose(f);
    
    // Downmix to mono, convert to float [-1,1]
    std::vector<float> mono;
    mono.reserve(pcm.size() / num_channels);
    for (size_t i = 0; i < pcm.size(); i += num_channels) {
        float sum = 0;
        for (int c = 0; c < num_channels; c++) sum += pcm[i + c] / 32768.0f;
        mono.push_back(sum / num_channels);
    }
    return mono;
}

// ---- JNI Methods (MUST match WhisperTranscriber.kt declarations) ----

extern "C" JNIEXPORT jlong JNICALL
Java_com_audioscribe_poc_data_transcription_WhisperTranscriber_whisperInit(
    JNIEnv* env, jobject, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    struct whisper_context_params params = whisper_context_default_params();
    struct whisper_context* ctx = whisper_init_from_file_with_params(path, params);
    env->ReleaseStringUTFChars(modelPath, path);
    if (!ctx) LOGE("whisper_init failed for %s", path);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_audioscribe_poc_data_transcription_WhisperTranscriber_whisperTranscribe(
    JNIEnv* env, jobject, jlong ctxPtr, jstring audioPath, jstring language) {
    auto* ctx = reinterpret_cast<whisper_context*>(ctxPtr);
    if (!ctx) return env->NewStringUTF("Error: Whisper not initialized");

    const char* audio = env->GetStringUTFChars(audioPath, nullptr);
    const char* lang = env->GetStringUTFChars(language, nullptr);
    
    std::vector<float> pcm = load_wav_16k_mono(audio);
    if (pcm.empty()) {
        env->ReleaseStringUTFChars(audioPath, audio);
        env->ReleaseStringUTFChars(language, lang);
        return env->NewStringUTF("Error: Audio load failed (WAV 16-bit PCM only)");
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = lang;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    
    int ret = whisper_full(ctx, params, pcm.data(), pcm.size());
    if (ret != 0) {
        env->ReleaseStringUTFChars(audioPath, audio);
        env->ReleaseStringUTFChars(language, lang);
        return env->NewStringUTF("Error: Inference failed");
    }

    std::string result;
    int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; ++i) {
        result += whisper_full_get_segment_text(ctx, i);
    }

    env->ReleaseStringUTFChars(audioPath, audio);
    env->ReleaseStringUTFChars(language, lang);
    
    if (result.empty()) result = "[No speech detected]";
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_audioscribe_poc_data_transcription_WhisperTranscriber_whisperFree(
    JNIEnv*, jobject, jlong ctxPtr) {
    auto* ctx = reinterpret_cast<whisper_context*>(ctxPtr);
    if (ctx) whisper_free(ctx);
}
