

cmake_minimum_required(VERSION 3.4.1)


add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)
add_library(
        libjpeg
        STATIC
        IMPORTED
)
set_target_properties(libjpeg PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/libturbojpeg.a)


include_directories(include)
target_link_libraries( # Specifies the target library.
        native-lib
        libjpeg
        jnigraphics
        ${log-lib})