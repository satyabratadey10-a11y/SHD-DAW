cmake_minimum_required(VERSION 3.22)
project("native-lib")

find_package(oboe REQUIRED CONFIG)

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp
        AudioEngine.cpp
        SamplerPlugin.cpp
        VolumePlugin.cpp
        Mixer.cpp
        WavWriter.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        oboe::oboe
        log
)
