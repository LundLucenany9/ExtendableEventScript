//
// Created by Admin on 22.10.2026.
//

#include "ees/main.h"

#include <iostream>

#include "ees/Lexer.h"
#include "ees/Parser.h"
#include "ees/Token.h"

int main() {
    const std::string source =
       "import std\n"
       "log \"Hello World\"\n"
       "for rovar i -> 0-10\n"
       "  log \"test\"";

    Lexer lexer(source);
    std::vector<Token> tokens = lexer.scanTokens();
    for (const Token& token : tokens) {
        std::cout << static_cast<int>(token.type)
          << " | " << token.lexeme
          << " | Zeile " << token.line
          << ", Spalte " << token.column
          << '\n';
    }

    Parser parser(tokens);
    ees::ast::Program program = parser.parse();
    for (ees::ast::StmtPtr& statement : program.statements) {
        if (auto* es = dynamic_cast<ees::ast::ExpressionStatement*>(statement.get())) {
            if (const auto* fc = dynamic_cast<ees::ast::FunctionCall*>(es->expression.get())) {
                std::cout << fc->fullyQualifiedName << "\n";
            }
        }
    }
    return 0;
}
