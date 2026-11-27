cmake_minimum_required(VERSION 3.22.1)

project("aicon-llama" LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED true)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED true)

# llama.cpp v1.1.0, pinned by hash. Downloaded at configure time so the
# engine source never needs a git submodule.
include(FetchContent)
FetchContent_Declare(
    llama
    URL https://github.com/ggml-org/llama.cpp/archive/refs/tags/v1.1.0.tar.gz
    URL_HASH SHA256=370603b8d973f4c02cd68ff2b815859c0f71878a57075cf2758015ceb7fc7990
)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(llama)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_SYSTEM_ARCH "ARM")
    set(GGML_CPU_KLEIDIAI ON)
    set(GGML_OPENMP ON)
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(GGML_SYSTEM_ARCH "x86")
    set(GGML_CPU_KLEIDIAI OFF)
    set(GGML_OPENMP OFF)
else()
    message(FATAL_ERROR "Unsupported ABI: ${ANDROID_ABI}")
endif()

add_library(${CMAKE_PROJECT_NAME} SHARED
        aicon-llama.cpp)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${llama_SOURCE_DIR}
        ${llama_SOURCE_DIR}/common
        ${llama_SOURCE_DIR}/include
        ${llama_SOURCE_DIR}/ggml/include
        ${llama_SOURCE_DIR}/ggml/src)

target_link_libraries(${CMAKE_PROJECT_NAME}
        llama
        llama-common
        android
        log)
