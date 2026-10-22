cmake_minimum_required(VERSION 3.20)

project(EES LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(ees
        src/Lexer.cpp
        src/main.cpp
        include/ees/TokenType.h
        include/ees/Token.h
        include/ees/ast/Ast.h
        src/Parser.cpp
)

target_include_directories(ees PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)