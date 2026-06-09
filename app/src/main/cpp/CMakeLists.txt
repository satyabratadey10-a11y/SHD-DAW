cmake_minimum_required(VERSION 3.22.1)
project("soniccore")

add_library(
        soniccore
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        soniccore
        ${log-lib})
