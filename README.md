# AudioScribe PoC - Offline Whisper AI Transcription App

A production-ready Proof of Concept Android app demonstrating offline speech-to-text transcription using Whisper.cpp, with first-launch model download capabilities.

## Features

- **🎯 First-Launch Model Download**: Downloads Whisper `ggml-base.bin` (~74MB) from Hugging Face on initial launch using OkHttp with real-time progress tracking
- **🎤 Dual Audio Input**: 
  - Live microphone recording (MediaRecorder, AAC/M4A format)
  - File import via Android's system file picker (audio/*)
- **🧠 Offline Whisper Transcription**: 100% on-device inference using whisper.cpp via JNI (mock implementation included)
- **💾 Local Room Database**: Persistent storage with full-text search (`LIKE '%query%'`)
- **🔍 Real-time Search**: Instant keyword search across transcript text and titles
- **🎨 Material 3 Design**: Modern UI with dynamic colors, dark/light theme support
- **📱 MVVM Architecture**: Clean separation of concerns with ViewModel, Repository pattern

## Architecture

```
app/
├── data/
│   ├── audio/           # AudioHandler - Recording & Import
│   ├── db/              # Room Database (Entity, DAO, Database)
│   ├── download/        # ModelDownloadManager - OkHttp streaming
│   ├── model/           # TranscriptEntity
│   └── transcription/   # WhisperTranscriber - JNI wrapper
├── ui/
│   ├── screens/         # Compose Screens (Home, Record, ModelSetup)
│   ├── theme/           # Material 3 Theme (Colors, Typography, Shapes)
│   └── viewmodel/       # AudioScribeViewModel
└── MainActivity.kt      # Navigation & Permissions
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose (Material 3) |
| Architecture | MVVM + Repository |
| Database | Room (SQLite) |
| Networking | OkHttp 4.12 |
| Concurrency | Kotlin Coroutines + Flow |
| AI Engine | whisper.cpp (JNI) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 16) |

## Permissions

```xml
<!-- Required for model download on first launch -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Required for live microphone recording -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Storage permissions for audio file import -->
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
```

## Building the Project

### Prerequisites
- Android Studio Koala (2024.1.1) or later
- JDK 21
- Android SDK 36

### Gradle Setup
The project uses version catalogs (`gradle/libs.versions.toml`) for dependency management.

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## Whisper.cpp Integration (Production)

For a production build with real Whisper inference:

1. **Add whisper.cpp as CMake dependency** in `app/CMakeLists.txt`:
```cmake
add_subdirectory(whisper.cpp)
target_link_libraries(audioscribe whisper)
```

2. **Create JNI bindings** in `app/src/main/cpp/whisper_jni.cpp`:
```cpp
extern "C" JNIEXPORT jlong JNICALL
Java_com_audioscribe_poc_data_transcription_WhisperTranscriber_whisperInit(
    JNIEnv* env, jobject thiz, jstring modelPath) {
    // Initialize whisper context
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_audioscribe_poc_data_transcription_WhisperTranscriber_whisperTranscribe(
    JNIEnv* env, jobject thiz, jlong context, jstring audioPath, jstring language) {
    // Run inference
}
```

3. **Update WhisperTranscriber.kt** to use actual JNI methods instead of mock implementation.

## Model Download

The app downloads the Whisper base model from:
```
https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin
```

Model is stored in: `context.filesDir/models/ggml-base.bin`

## Project Structure Details

### Data Layer
- **AudioHandler**: Manages MediaRecorder for live recording, handles file import via ContentResolver
- **ModelDownloadManager**: Streams model download with progress callbacks using OkHttp
- **WhisperTranscriber**: JNI wrapper for whisper.cpp (currently mock implementation)
- **TranscriptDatabase**: Room database with full-text search capability

### UI Layer
- **ModelSetupScreen**: First-launch experience with download progress
- **HomeScreen**: Transcript list with search, record/import buttons
- **RecordScreen**: Live recording interface with real-time duration and transcription

### ViewModel
- **AudioScribeViewModel**: Central state management coordinating all operations

## Key Implementation Notes

1. **Model Download**: Uses OkHttp streaming with progress updates via Kotlin Channels
2. **Audio Format**: Records at 16kHz mono AAC (optimal for Whisper)
3. **Database**: Room with Flow for reactive UI updates
4. **Search**: Implemented via SQLite `LIKE` queries on transcript text and title
5. **Navigation**: Compose Navigation with three destinations

## Future Enhancements

- [ ] Real whisper.cpp JNI integration
- [ ] Multiple model support (tiny, base, small, medium)
- [ ] Language detection/selection
- [ ] Export transcripts (TXT, SRT, JSON)
- [ ] Background transcription with WorkManager
- [ ] Audio playback of recordings
- [ ] Speaker diarization
- [ ] Translation support

## License

This is a Proof of Concept project for demonstration purposes.
