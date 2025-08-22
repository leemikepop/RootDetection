cmake_minimum_required(VERSION 3.4.1)

# Add this line to set the linker flag for 16KB page alignment
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(
        native-lib
        SHARED
        native-lib.c
)

find_library(
        log-lib
        log
)

target_link_libraries(
        native-lib
        ${log-lib}
)
