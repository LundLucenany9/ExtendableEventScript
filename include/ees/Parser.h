
#pragma once

#include "ees/Token.h"
#include "ees/ast/Ast.h"

#include <string>
#include <unordered_set>
#include <vector>

class Parser {
public:
    explicit Parser(std::vector<Token> tokens);

    ees::ast::Program parse();

    // Wird auch zum Parsen von Ausdrücken in interpolierten Strings benötigt.
    ees::ast::ExprPtr parseExpressionOnly();

private:
    std::vector<Token> tokens;
    std::size_t current = 0;

    std::string scope = "local";
    std::unordered_set<std::string> definedNames;
    std::unordered_set<std::string> functionNames;
    std::unordered_set<std::string> functionParameters;

    // Statements
    ees::ast::ImportStatement parseImport();
    ees::ast::StmtPtr parseStatement();
    ees::ast::StmtPtr parseVar(bool isGlobal, bool isReadOnly);
    ees::ast::StmtPtr parseFunction();
    ees::ast::StmtPtr parseGlobalFunction();
    std::unique_ptr<ees::ast::FunctionDeclaration> parseFuncBody(
        const std::string& name
    );
    ees::ast::StmtPtr parseIf();
    ees::ast::StmtPtr parseFor();
    ees::ast::StmtPtr parseForEach();
    ees::ast::StmtPtr parseReturn();
    ees::ast::StmtPtr parseExpressionStatement();

    // Expressions
    ees::ast::ExprPtr parseExpression();
    ees::ast::ExprPtr parseAssignment();
    ees::ast::ExprPtr parseOr();
    ees::ast::ExprPtr parseAnd();
    ees::ast::ExprPtr parseEquality();
    ees::ast::ExprPtr parseComparison();
    ees::ast::ExprPtr parseAdditive();
    ees::ast::ExprPtr parseMultiplicative();
    ees::ast::ExprPtr parseUnary();
    ees::ast::ExprPtr parsePostfix();
    ees::ast::ExprPtr parsePrimary();

    ees::ast::ExprPtr parseFunctionCallOrVariable();
    ees::ast::ExprPtr parseArrayLiteral();
    ees::ast::ExprPtr parseMapLiteral();
    ees::ast::RangeLiteral parseRange();
    ees::ast::ExprPtr parseInterpolatedString(const std::string& literal) const;

    // Parser state and errors
    void registerVariable(const std::string& name);
    void unregisterVariable(const std::string& name);
    void unregisterFuncVariables();
    void registerFunction(const std::string& name);

    bool match(TokenType type);
    bool match(std::initializer_list<TokenType> types);
    bool check(TokenType type) const;
    Token advance();
    bool isAtEnd() const;
    const Token& peek() const;
    const Token& previous() const;
    const Token& consume(TokenType type, const std::string& message);
    const Token& expect(TokenType type);
    void skipNewlines();

    bool canStartExpression() const;
    bool canContinueArguments() const;

    [[noreturn]] static void error(const Token& token, const std::string& message);
};
