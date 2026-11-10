cmake_minimum_required(VERSION 3.22.1)
project("whisper_jni" LANGUAGES CXX)

include(FetchContent)
FetchContent_Declare(
  whisper
  GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp
  GIT_TAG        v1.5.4
)
FetchContent_MakeAvailable(whisper)

add_library(whisper_jni SHARED whisper_jni.cpp)
target_link_libraries(whisper_jni PRIVATE whisper ${log-lib})
target_include_directories(whisper_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${whisper_SOURCE_DIR}/include
    ${whisper_BINARY_DIR}/include
)
