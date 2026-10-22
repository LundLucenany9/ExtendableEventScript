//
// Created by Admin on 22.10.2026.
//

#include "ees/main.h"

#include <iostream>

#include "ees/Lexer.h"
#include "ees/Token.h"

int main() {
    const std::string source =
       "import std\n"
       "log \"Hello World\"\n"
       "for rovar i -> 0-10\n";

    Lexer lexer(source);
    for (const auto tokens = lexer.scanTokens(); const Token& token : tokens) {
        std::cout << static_cast<int>(token.type)
          << " | " << token.lexeme
          << " | Zeile " << token.line
          << ", Spalte " << token.column
          << '\n';
    }

    return 0;
}
