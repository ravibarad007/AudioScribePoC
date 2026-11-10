cmake_minimum_required(VERSION 3.22.1)
project("whisper_jni" LANGUAGES CXX)

# 1. Fetch whisper.cpp at configure time
include(FetchContent)
FetchContent_Declare(
  whisper
  GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp
  GIT_TAG        v1.5.4
)
FetchContent_MakeAvailable(whisper)

# 2. Verify whisper target exists (it's called "whisper", not "whisper::whisper")
if(NOT TARGET whisper)
    message(FATAL_ERROR "whisper target not found! Check whisper.cpp CMakeLists.txt")
endif()

# 3. Our JNI wrapper
add_library(whisper_jni SHARED whisper_jni.cpp)

# 3. Link to whisper.cpp's target (name is "whisper", not "whisper::whisper")
target_link_libraries(whisper_jni PRIVATE whisper ${log-lib})

# 4. Include directories (whisper.cpp headers)
target_include_directories(whisper_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${whisper_SOURCE_DIR}/include
    ${whisper_BINARY_DIR}/include
)
