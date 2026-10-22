//
// Created by Admin on 22.10.2026.
//

#pragma once

#include "Token.h"

#include <string>
#include <vector>
#include <cstddef>

class Lexer {
    public:
        explicit Lexer(std::string  source);

        std::vector<Token> scanTokens();
    private:
        std::string source;
        std::vector<Token> tokens;

        std::vector<int> indents;

        std::size_t start = 0;
        std::size_t current = 0;

        std::size_t line = 1;
        std::size_t column = 1;
        std::size_t tokenColumn = 1;

        bool isAtEnd() const;
        char advance();
        char peek() const;
        char peekNext() const;
        bool match(char expected);

        void scanToken();
        void scanIdentifier();
        void scanNumber();
        void scanString();
        void addToken(TokenType type);

        void addIndent();
        void addDedent();

        void skipWhitespace();

        static bool isIdentifierStart(char c);
};
