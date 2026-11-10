cmake_minimum_required(VERSION 3.22.1)
project("whisper_jni" LANGUAGES CXX)

# 1. Use whisper.cpp as SUBDIRECTORY (not FetchContent) — guarantees target visibility
# We'll clone it at configure time via ExternalProject (works in CI)
include(ExternalProject)

# 2. Download & build whisper.cpp as external project
ExternalProject_Add(
    whisper_external
    GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp
    GIT_TAG v1.5.4
    SOURCE_DIR ${CMAKE_BINARY_DIR}/whisper-src
    BINARY_DIR ${CMAKE_BINARY_DIR}/whisper-build
    CMAKE_ARGS
        -DCMAKE_CXX_COMPILER=${CMAKE_CXX_COMPILER}
        -DCMAKE_C_COMPILER=${CMAKE_C_COMPILER}
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DCMAKE_BUILD_TYPE=Release
        -DBUILD_SHARED_LIBS=ON
        -DWHISPER_BUILD_TESTS=OFF
        -DWHISPER_BUILD_EXAMPLES=OFF
    BUILD_IN_SOURCE 0
    INSTALL_DIR ${CMAKE_BINARY_DIR}/whisper-install
)

# 2. Get whisper library location (built by ExternalProject)
ExternalProject_Get_Property(whisper_external INSTALL_DIR)
set(WHISPER_INSTALL_DIR ${INSTALL_DIR})

# 3. Our JNI wrapper — depends on whisper_external
add_library(whisper_jni SHARED whisper_jni.cpp)

# Link to whisper library (built by ExternalProject)
add_dependencies(whisper_jni whisper_external)
target_link_libraries(whisper_jni
    ${WHISPER_INSTALL_DIR}/lib/libwhisper.so
    ${log-lib}
)

# Include dirs
target_include_directories(whisper_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${WHISPER_INSTALL_DIR}/include
)
