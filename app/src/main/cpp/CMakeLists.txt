Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.22.1)
Declares and names the project.
project("keepalivenative")
Creates and names a library, sets it as either STATIC or SHARED
add_library(
native-engine
SHARED
native-engine.c)
Searches for a specified prebuilt library (Android Log)
find_library(
log-lib
log)
Links your native library against the log library
target_link_libraries(
native-engine
${log-lib})
