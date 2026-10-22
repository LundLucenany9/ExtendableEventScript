//
// Created by Admin on 22.10.2026.
//

#pragma once

enum class TokenType {
    GLOBAL,
    VAR,
    ROVAR,
    FUNC,
    IF,
    ELIF,
    ELSE,
    FOREACH,
    FOR,
    RETURN,

    IDENTIFIER,
    DECIMAL,
    INTEGER,
    STRING,
    INTERPOLATED_STRING,
    NULL_,
    TRUE,
    FALSE,

    OR,
    AND,

    EQUAL,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    NOT,
    LESS,
    GREATER,
    EQUAL_EQUAL,
    NOT_EQUAL,
    LESS_EQUAL,
    GREATER_EQUAL,

    DOLLAR,
    HASH,
    DOT,
    ARROW,

    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    LEFT_BRACE,
    RIGHT_BRACE,
    COMMA,

    INDENT,
    DEDENT,
    NEWLINE,

    EOF_
};