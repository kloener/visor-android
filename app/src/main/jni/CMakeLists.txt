cmake_minimum_required(VERSION 3.4.1)

add_library(yuv-decoder SHARED
            yuv-decoder.c)

# Include libraries needed for hello-jni lib
target_link_libraries(yuv-decoder
                      android
                      log)