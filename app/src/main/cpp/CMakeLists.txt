cmake_minimum_required(VERSION 3.22.1)
project(ps2bridge LANGUAGES CXX)

add_library(ps2bridge SHARED ps2bridge.cpp)
target_compile_features(ps2bridge PRIVATE cxx_std_17)
target_compile_options(ps2bridge PRIVATE -Wall -Wextra -Werror)
