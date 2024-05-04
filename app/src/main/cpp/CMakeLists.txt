
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("ircamera")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.

#set(NDK_ROOT "android-ndk")

#set(LIBCXX_PATH "${NDK_ROOT}/sources/cxx-stl/llvm-libc++")
#set(LIBCXXABI_PATH "${NDK_ROOT}/sources/cxx-stl/llvm-libc++abi")
#message("C++LIBS:" ${LIBCXXABI_PATH})
#include_directories(${LIBCXX_PATH}/include
#        ${LIBCXXABI_PATH}/include)
#link_directories(${LIBCXX_PATH}/libs/${CMAKE_ANDROID_ARCH_ABI}
#        ${LIBCXXABI_PATH}/libs/${CMAKE_ANDROID_ARCH_ABI})
set(SRC_PATH ${CMAKE_CURRENT_SOURCE_DIR})
set(LIBRARIES_INCLUDE_PATH ${SRC_PATH}/3rdParty)
set(LIBRARIES_PATH ${SRC_PATH}/../jniLibs)
file(GLOB SRC_FILES
        ${SRC_PATH}/*.h
        ${SRC_PATH}/*.hpp
        ${SRC_PATH}/*.cpp)
# opencv
include_directories(${LIBRARIES_INCLUDE_PATH}/opencv48)
add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION
        ${LIBRARIES_PATH}/${CMAKE_ANDROID_ARCH_ABI}/libopencv_java4.so)

add_library(${CMAKE_PROJECT_NAME} SHARED
    # List C/C++ source files with relative paths to this CMakeLists.txt.
    ${SRC_FILES})

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
    # List libraries link to the target library
    android
    log
    opencv_java4)
