cmake_minimum_required(VERSION 3.10)

project(asio-sdk)

file(GLOB_RECURSE SRC_FILES asio-src/common/*.cpp)
file(GLOB_RECURSE PLT_FILES asio-src/host/*.cpp)


set(FILE_TO_REMOVE "${CMAKE_CURRENT_SOURCE_DIR}/asio-src/common/asiodrvr.cpp")
list(REMOVE_ITEM SRC_FILES ${FILE_TO_REMOVE})
set(FILE_TO_REMOVE "${CMAKE_CURRENT_SOURCE_DIR}/asio-src/common/asiodrvr.h")
list(REMOVE_ITEM SRC_FILES ${FILE_TO_REMOVE})
set(FILE_TO_REMOVE "${CMAKE_CURRENT_SOURCE_DIR}/asio-src/common/dllentry.cpp")
list(REMOVE_ITEM SRC_FILES ${FILE_TO_REMOVE})
set(FILE_TO_REMOVE "${CMAKE_CURRENT_SOURCE_DIR}/asio-src/host/sample/hostsample.cpp")
list(REMOVE_ITEM PLT_FILES ${FILE_TO_REMOVE})

add_library(asio-sdk STATIC ${SRC_FILES} ${PLT_FILES})

target_include_directories(asio-sdk PUBLIC asio-src/common asio-src/host asio-src/host/pc)
