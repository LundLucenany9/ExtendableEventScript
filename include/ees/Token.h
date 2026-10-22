//
// Created by Admin on 22.10.2026.
//

#pragma once

#include <string>

#include "TokenType.h"

struct Token {
    TokenType type;
    std::string lexeme;
    std::size_t line;
    std::size_t column;

    Token(TokenType type, std::string lexeme, size_t line, size_t column):
        type(type),
        lexeme(std::move(lexeme)),
        line(line),
        column(column)
    {}
};