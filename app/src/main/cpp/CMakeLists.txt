cmake_minimum_required(VERSION 3.22.1)
project("soniccore")

add_library(
        soniccore
        SHARED
        native-lib.cpp
        AutomationManager.cpp
        Mixer.cpp
        AudioEngine.cpp)

find_package(oboe REQUIRED CONFIG)

find_library(
        log-lib
        log)

target_link_libraries(
        soniccore
        oboe::oboe
        ${log-lib})
