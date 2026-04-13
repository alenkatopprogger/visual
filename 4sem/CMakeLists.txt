cmake_minimum_required(VERSION 3.14)
project(backend_notes)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(THIRD_PARTY_DIR ${PROJECT_SOURCE_DIR}/third_party)
set(SRC_DIR ${PROJECT_SOURCE_DIR}/src)
set(INCLUDE_DIR ${PROJECT_SOURCE_DIR}/include)

# SDL2
find_package(SDL2 REQUIRED)
include_directories(${SDL2_INCLUDE_DIRS})

# OpenGL/GLEW
find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)

# ZMQ - улучшенный поиск
find_package(PkgConfig QUIET)
if(PkgConfig_FOUND)
    pkg_check_modules(ZMQ libzmq)
endif()

if(NOT ZMQ_FOUND)
    # Пробуем найти стандартные пути
    find_path(ZMQ_INCLUDE_DIR NAMES zmq.hpp zmq.h 
        PATHS /usr/include /usr/local/include /opt/local/include)
    find_library(ZMQ_LIBRARY NAMES zmq libzmq
        PATHS /usr/lib /usr/local/lib /opt/local/lib)
    
    if(ZMQ_INCLUDE_DIR AND ZMQ_LIBRARY)
        set(ZMQ_FOUND TRUE)
        set(ZMQ_INCLUDE_DIRS ${ZMQ_INCLUDE_DIR})
        set(ZMQ_LIBRARIES ${ZMQ_LIBRARY})
        message(STATUS "ZMQ found manually: ${ZMQ_LIBRARIES}")
    endif()
endif()

if(ZMQ_FOUND)
    include_directories(${ZMQ_INCLUDE_DIRS})
    message(STATUS "ZMQ found: ${ZMQ_LIBRARIES}")
else()
    message(FATAL_ERROR "ZMQ not found. Please install libzmq3-dev or zmq")
endif()

# nlohmann/json
find_package(nlohmann_json QUIET)
if(NOT nlohmann_json_FOUND)
    if(EXISTS ${THIRD_PARTY_DIR}/json/include/nlohmann/json.hpp)
        set(NLOHMANN_INCLUDE_DIR ${THIRD_PARTY_DIR}/json/include)
        message(STATUS "Using nlohmann/json from third_party")
    else()
        message(FATAL_ERROR "nlohmann/json not found! Please install it or add to third_party/json/include")
    endif()
else()
    message(STATUS "nlohmann/json found via find_package")
endif()

include_directories(${INCLUDE_DIR})

# imgui
add_library(imgui STATIC
    ${THIRD_PARTY_DIR}/imgui/imgui.cpp
    ${THIRD_PARTY_DIR}/imgui/imgui_draw.cpp
    ${THIRD_PARTY_DIR}/imgui/imgui_demo.cpp
    ${THIRD_PARTY_DIR}/imgui/imgui_tables.cpp
    ${THIRD_PARTY_DIR}/imgui/imgui_widgets.cpp
    ${THIRD_PARTY_DIR}/imgui/backends/imgui_impl_sdl2.cpp
    ${THIRD_PARTY_DIR}/imgui/backends/imgui_impl_opengl3.cpp
)
target_include_directories(imgui PUBLIC ${THIRD_PARTY_DIR}/imgui)
target_compile_options(imgui PRIVATE -fPIC)

# implot
add_library(implot STATIC
    ${THIRD_PARTY_DIR}/implot/implot.cpp
    ${THIRD_PARTY_DIR}/implot/implot_demo.cpp
    ${THIRD_PARTY_DIR}/implot/implot_items.cpp
)
target_include_directories(implot PUBLIC ${THIRD_PARTY_DIR}/implot)
target_link_libraries(implot PRIVATE imgui)
target_compile_options(implot PRIVATE -fPIC)

# Основной исполняемый файл
add_executable(main ${SRC_DIR}/main.cpp)

# Линковка библиотек
target_link_libraries(main PRIVATE 
    imgui 
    implot 
    ${SDL2_LIBRARIES} 
    ${OPENGL_LIBRARIES} 
    ${GLEW_LIBRARIES}
    ${ZMQ_LIBRARIES}
)

# Добавляем include директории
target_include_directories(main PRIVATE
    ${SDL2_INCLUDE_DIRS}
    ${GLEW_INCLUDE_DIRS}
    ${OPENGL_INCLUDE_DIRS}
    ${ZMQ_INCLUDE_DIRS}
    ${INCLUDE_DIR}
)

# Добавляем nlohmann/json include
if(nlohmann_json_FOUND)
    target_link_libraries(main PRIVATE nlohmann_json::nlohmann_json)
else()
    target_include_directories(main PRIVATE ${NLOHMANN_INCLUDE_DIR})
endif()

# Опционально: добавляем флаги компиляции
target_compile_options(main PRIVATE -Wall -Wextra)

# Для отладки: выводим информацию о найденных библиотеках
message(STATUS "SDL2_LIBRARIES: ${SDL2_LIBRARIES}")
message(STATUS "OPENGL_LIBRARIES: ${OPENGL_LIBRARIES}")
message(STATUS "GLEW_LIBRARIES: ${GLEW_LIBRARIES}")
message(STATUS "ZMQ_LIBRARIES: ${ZMQ_LIBRARIES}")