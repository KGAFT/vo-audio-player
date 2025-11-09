cmake_minimum_required(VERSION 3.10)

project(vo-asio-backend)

add_subdirectory(modules/asio)
add_executable(vo-asio-backend test.cpp)

target_link_libraries(vo-asio-backend PUBLIC asio-sdk)