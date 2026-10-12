cmake_minimum_required(VERSION 3.22.1)
project(whisper_jni C CXX)

set(CMAKE_CXX_STANDARD 17)

# Build whisper.cpp + ggml as static libraries linked into one small JNI library.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_ALL_WARNINGS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
add_subdirectory(whisper.cpp)

add_library(whisper_jni SHARED whisper_jni.cpp)
target_link_libraries(whisper_jni PRIVATE whisper log)
target_compile_options(whisper_jni PRIVATE -O3 -fvisibility=hidden)
target_link_options(whisper_jni PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
