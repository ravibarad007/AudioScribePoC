cmake_minimum_required(VERSION 3.22.1)
project("whisper_jni" LANGUAGES CXX)

# 1. Fetch whisper.cpp
include(FetchContent)
FetchContent_Declare(
  whisper
  GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp
  GIT_TAG        v1.5.4
)
FetchContent_MakeAvailable(whisper)

# 2. Verify whisper target name (whisper.cpp uses "whisper" not "whisper::whisper")
# Check: ${whisper_SOURCE_DIR}/CMakeLists.txt defines: add_library(whisper ...)

# 3. JNI wrapper
add_library(whisper_jni SHARED whisper_jni.cpp)

# 4. Link to whisper.cpp's actual target name ("whisper")
target_link_libraries(whisper_jni PRIVATE whisper ${log-lib})

# 4. Include directories
target_include_directories(whisper_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${whisper_SOURCE_DIR}/include
    ${whisper_BINARY_DIR}/include
)
