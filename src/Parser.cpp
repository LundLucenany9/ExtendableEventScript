
#include "ees/Parser.h"

#include <algorithm>

#include "ees/Lexer.h"

#include <cctype>
#include <cstdint>
#include <stdexcept>
#include <utility>

using namespace ees::ast;

namespace {

std::string decodeEscapes(const std::string& input) {
    std::string result;
    result.reserve(input.size());

    for (std::size_t i = 0; i < input.size(); ++i) {
        if (input[i] == '\\' && i + 1 < input.size()) {
            switch (input[i + 1]) {
                case 'n': result += '\n'; ++i; continue;
                case 't': result += '\t'; ++i; continue;
                case 'r': result += '\r'; ++i; continue;
                case '\\': result += '\\'; ++i; continue;
                case '"': result += '"'; ++i; continue;
                default: break;
            }
        }
        result += input[i];
    }

    return result;
}

template<typename T>
std::unique_ptr<T> makeNode() {
    return std::make_unique<T>();
}

} // namespace

Parser::Parser(std::vector<Token> tokens)
    : tokens(std::move(tokens)) {
    if (this->tokens.empty() ||
        this->tokens.back().type != TokenType::EOF_) {
        throw std::invalid_argument(
            "Parser requires an EOF_ token"
        );
    }
}

// ------------------------------------------------------------
// Program
// ------------------------------------------------------------

Program Parser::parse() {
    Program program;

    skipNewlines();

    while (match(TokenType::IMPORT)) {
        program.imports.push_back(
            std::make_unique<ImportStatement>(parseImport())
        );
        skipNewlines();
    }

    while (!isAtEnd()) {
        skipNewlines();
        if (isAtEnd()) break;

        program.statements.push_back(parseStatement());
    }

    return program;
}

ExprPtr Parser::parseExpressionOnly() {
    ExprPtr expression = parseExpression();
    skipNewlines();

    if (!isAtEnd()) {
        error(peek(), "Unexpected token after expression");
    }

    return expression;
}

ImportStatement Parser::parseImport() {
    std::string module =
        consume(TokenType::IDENTIFIER, "Expected module name").lexeme;

    while (match(TokenType::DOT)) {
        module += '.';
        module += consume(
            TokenType::IDENTIFIER,
            "Expected module name after '.'"
        ).lexeme;
    }

    skipNewlines();

    ImportStatement result;
    result.module = std::move(module);
    return result;
}

// ------------------------------------------------------------
// Statements and scopes
// ------------------------------------------------------------

StmtPtr Parser::parseStatement() {
    skipNewlines();

    if (match(TokenType::GLOBAL)) {
        const std::string previousScope = scope;
        scope = "glob";

        StmtPtr result;

        if (match(TokenType::VAR)) {
            result = parseVar(true, false);
        } else if (match(TokenType::ROVAR)) {
            result = parseVar(true, true);
        } else {
            error(peek(), "Expected 'var' or 'rovar' after 'global'");
        }

        scope = previousScope;
        return result;
    }

    if (match(TokenType::VAR)) return parseVar(false, false);
    if (match(TokenType::ROVAR)) return parseVar(false, true);
    if (match(TokenType::FUNC)) return parseFunction();
    if (match(TokenType::IF)) return parseIf();
    if (match(TokenType::FOR)) return parseFor();
    if (match(TokenType::FOREACH)) return parseForEach();
    if (match(TokenType::RETURN)) return parseReturn();

    return parseExpressionStatement();
}

void Parser::registerVariable(const std::string& name) {
    const std::string fullName = scope + "." + name;

    if (scope == "func" && functionParameters.contains(name)) {
        error(
            peek(),
            "Parameter '" + name +
            "' cannot be redefined as a variable"
        );
    }

    if (definedNames.contains(fullName)) {
        error(
            peek(),
            scope + " variable '" + name + "' is already defined"
        );
    }

    definedNames.insert(fullName);
}

void Parser::unregisterVariable(const std::string& name) {
    definedNames.erase(scope + "." + name);
}

void Parser::unregisterFuncVariables() {
    for (auto it = definedNames.begin(); it != definedNames.end();) {
        if (it->starts_with("func.")) {
            it = definedNames.erase(it);
        } else {
            ++it;
        }
    }
}

void Parser::registerFunction(const std::string& name) {
    const std::string fullName = scope + "." + name;

    if (functionNames.contains(fullName)) {
        error(
            peek(),
            scope + " function '" + name + "' is already defined"
        );
    }

    functionNames.insert(fullName);
}

StmtPtr Parser::parseVar(const bool isGlobal, const bool isReadOnly) {
    const std::string name =
        expect(TokenType::IDENTIFIER).lexeme;

    expect(TokenType::EQUAL);
    registerVariable(name);

    auto initializer = parseExpression();
    skipNewlines();

    auto result = makeNode<VarDeclaration>();
    result->name = (isGlobal ? "glob" : scope) + "." + name;
    result->initializer = std::move(initializer);
    result->isReadOnly = isReadOnly;

    return result;
}

StmtPtr Parser::parseFunction() {
    std::string name = expect(TokenType::IDENTIFIER).lexeme;
    name = scope + "." + name;
    registerFunction(name);
    return parseFuncBody(name);
}

StmtPtr Parser::parseGlobalFunction() {
    std::string name = expect(TokenType::IDENTIFIER).lexeme;
    name = scope + "." + name;
    registerFunction(name);

    auto func = parseFuncBody(name);
    func->isGlobal = true;
    return func;
}

std::unique_ptr<FunctionDeclaration> Parser::parseFuncBody(const std::string& name) {
    expect(TokenType::LEFT_PAREN);

    std::vector<FunctionParameter> parameters;
    std::map<std::string, ExprPtr> optionalParams;

    functionParameters.clear();

    if (!check(TokenType::RIGHT_PAREN)) {
        do {
            const bool isReference = match(TokenType::HASH);
            const std::string paramName =
                expect(TokenType::IDENTIFIER).lexeme;

            parameters.push_back({.name = paramName, .isReference = isReference});
            functionParameters.insert(paramName);
        } while (match(TokenType::COMMA));
    }

    expect(TokenType::RIGHT_PAREN);

    if (match(TokenType::LEFT_BRACE)) {
        if (!check(TokenType::RIGHT_BRACE)) {
            do {
                const std::string key =
                    expect(TokenType::IDENTIFIER).lexeme;

                expect(TokenType::EQUAL);
                optionalParams[key] = parseExpression();
            } while (match(TokenType::COMMA));
        }

        expect(TokenType::RIGHT_BRACE);
    }

    skipNewlines();
    expect(TokenType::INDENT);

    const std::string previousScope = scope;
    scope = "func";

    std::vector<StmtPtr> body;

    skipNewlines();
    while (!check(TokenType::DEDENT) && !isAtEnd()) {
        body.push_back(parseStatement());
    }

    unregisterFuncVariables();
    scope = previousScope;
    functionParameters.clear();

    expect(TokenType::DEDENT);
    skipNewlines();

    auto result = makeNode<FunctionDeclaration>();
    result->name = name;
    result->parameters = std::move(parameters);
    result->optionalParams = std::move(optionalParams);
    result->body = std::move(body);
    result->isGlobal = false;

    return result;
}

// ------------------------------------------------------------
// Control flow
// ------------------------------------------------------------

StmtPtr Parser::parseIf() {
    auto condition = parseExpression();
    skipNewlines();
    expect(TokenType::INDENT);

    auto result = makeNode<IfStatement>();
    result->condition = std::move(condition);

    while (!check(TokenType::DEDENT) && !isAtEnd()) {
        result->thenBody.push_back(parseStatement());
    }

    expect(TokenType::DEDENT);
    skipNewlines();

    while (match(TokenType::ELIF)) {
        IfElseClause clause;
        clause.condition = parseExpression();

        skipNewlines();
        expect(TokenType::INDENT);

        while (!check(TokenType::DEDENT) && !isAtEnd()) {
            clause.body.push_back(parseStatement());
        }

        expect(TokenType::DEDENT);
        skipNewlines();

        result->elifClauses.push_back(std::move(clause));
    }

    if (match(TokenType::ELSE)) {
        skipNewlines();
        expect(TokenType::INDENT);

        while (!check(TokenType::DEDENT) && !isAtEnd()) {
            result->elseBody.push_back(parseStatement());
        }

        expect(TokenType::DEDENT);
        skipNewlines();
    }

    return result;
}

StmtPtr Parser::parseFor() {
    match(TokenType::ROVAR);

    const std::string variable =
        expect(TokenType::IDENTIFIER).lexeme;

    registerVariable(variable);

    expect(TokenType::ARROW);
    const RangeLiteral range = parseRange();

    skipNewlines();
    expect(TokenType::INDENT);

    auto result = makeNode<ForLoop>();
    result->variable = variable;
    result->range = range;

    while (!check(TokenType::DEDENT) && !isAtEnd()) {
        result->body.push_back(parseStatement());
    }

    expect(TokenType::DEDENT);
    unregisterVariable(variable);
    skipNewlines();

    return result;
}

StmtPtr Parser::parseForEach() {
    expect(TokenType::ROVAR);

    // Erstes rovar: Indexvariable
    const std::string indexVar =
        expect(TokenType::IDENTIFIER).lexeme;
    registerVariable(indexVar);

    expect(TokenType::COMMA);
    expect(TokenType::ROVAR);

    // Zweites rovar: Elementvariable
    const std::string itemVar =
        expect(TokenType::IDENTIFIER).lexeme;
    registerVariable(itemVar);

    expect(TokenType::ARROW);
    auto iterable = parseExpression();

    skipNewlines();
    expect(TokenType::INDENT);

    auto result = makeNode<ForEachLoop>();
    result->indexVar = indexVar;
    result->itemVar = itemVar;
    result->iterable = std::move(iterable);

    while (!check(TokenType::DEDENT) && !isAtEnd()) {
        result->body.push_back(parseStatement());
    }

    expect(TokenType::DEDENT);

    unregisterVariable(itemVar);
    unregisterVariable(indexVar);
    skipNewlines();

    return result;
}

StmtPtr Parser::parseReturn() {
    ExprPtr value;

    if (!check(TokenType::NEWLINE) &&
        !check(TokenType::DEDENT) &&
        !isAtEnd() &&
        canStartExpression()) {
        value = parseExpression();
    }

    skipNewlines();

    auto result = makeNode<ReturnStatement>();
    result->value = std::move(value);
    return result;
}

StmtPtr Parser::parseExpressionStatement() {
    auto expression = parseExpression();
    skipNewlines();

    auto result = makeNode<ExpressionStatement>();
    result->expression = std::move(expression);
    return result;
}

// ------------------------------------------------------------
// Expressions: precedence from low to high
// assignment, or, and, equality, comparison,
// additive, multiplicative, unary, postfix, primary
// ------------------------------------------------------------

ExprPtr Parser::parseExpression() {
    return parseAssignment();
}

ExprPtr Parser::parseAssignment() {
    auto expr = parseOr();

    if (!match(TokenType::EQUAL)) {
        return expr;
    }

    auto value = parseExpression();

    if (const auto* variable = dynamic_cast<VariableAccess*>(expr.get())) {
        auto result = makeNode<Assignment>();
        result->variableName = variable->name;
        result->value = std::move(value);
        skipNewlines();
        return result;
    }

    if (auto* index = dynamic_cast<IndexAccess*>(expr.get())) {
        auto result = makeNode<IndexedAssignment>();
        result->target = std::move(index->target);
        result->index = std::move(index->index);
        result->value = std::move(value);
        skipNewlines();
        return result;
    }

    error(previous(), "Invalid assignment target");
}

ExprPtr Parser::parseOr() {
    auto expr = parseAnd();

    while (match(TokenType::OR)) {
        const TokenType op = previous().type;
        auto right = parseAnd();

        auto result = makeNode<BinaryOp>();
        result->left = std::move(expr);
        result->operatorType = op;
        result->right = std::move(right);
        expr = std::move(result);
    }

    return expr;
}

ExprPtr Parser::parseAnd() {
    auto expr = parseEquality();

    while (match(TokenType::AND)) {
        const TokenType op = previous().type;
        auto right = parseEquality();

        auto result = makeNode<BinaryOp>();
        result->left = std::move(expr);
        result->operatorType = op;
        result->right = std::move(right);
        expr = std::move(result);
    }

    return expr;
}

ExprPtr Parser::parseEquality() {
    auto expr = parseComparison();

    while (match({TokenType::EQUAL_EQUAL, TokenType::NOT_EQUAL})) {
        const TokenType op = previous().type;
        auto right = parseComparison();

        auto result = makeNode<BinaryOp>();
        result->left = std::move(expr);
        result->operatorType = op;
        result->right = std::move(right);
        expr = std::move(result);
    }

    return expr;
}

ExprPtr Parser::parseComparison() {
    auto expr = parseAdditive();

    while (match({
        TokenType::LESS,
        TokenType::LESS_EQUAL,
        TokenType::GREATER,
        TokenType::GREATER_EQUAL
    })) {
        const TokenType op = previous().type;
        auto right = parseAdditive();

        auto result = makeNode<BinaryOp>();
        result->left = std::move(expr);
        result->operatorType = op;
        result->right = std::move(right);
        expr = std::move(result);
    }

    return expr;
}

ExprPtr Parser::parseAdditive() {
    auto expr = parseMultiplicative();

    while (match({TokenType::PLUS, TokenType::MINUS})) {
        const TokenType op = previous().type;
        auto right = parseMultiplicative();

        auto result = makeNode<BinaryOp>();
        result->left = std::move(expr);
        result->operatorType = op;
        result->right = std::move(right);
        expr = std::move(result);
    }

    return expr;
}

ExprPtr Parser::parseMultiplicative() {
    auto expr = parseUnary();

    while (match({TokenType::STAR, TokenType::SLASH})) {
        const TokenType op = previous().type;
        auto right = parseUnary();

        auto result = makeNode<BinaryOp>();
        result->left = std::move(expr);
        result->operatorType = op;
        result->right = std::move(right);
        expr = std::move(result);
    }

    return expr;
}

ExprPtr Parser::parseUnary() {
    if (match({TokenType::NOT, TokenType::MINUS})) {
        const TokenType op = previous().type;
        auto operand = parseUnary();

        auto result = makeNode<UnaryOp>();
        result->operatorType = op;
        result->operand = std::move(operand);
        return result;
    }

    return parsePostfix();
}

ExprPtr Parser::parsePostfix() {
    auto expr = parsePrimary();

    while (match(TokenType::LEFT_BRACKET)) {
        auto index = parseExpression();

        if (!dynamic_cast<Literal*>(index.get()) &&
            !dynamic_cast<VariableAccess*>(index.get())) {
            error(previous(), "Index must be a literal or variable access");
        }

        expect(TokenType::RIGHT_BRACKET);

        auto result = makeNode<IndexAccess>();
        result->target = std::move(expr);
        result->index = std::move(index);
        expr = std::move(result);
    }

    return expr;
}

ExprPtr Parser::parsePrimary() {
    if (match(TokenType::NULL_)) {
        auto result = makeNode<Literal>();
        result->value = nullptr;
        return result;
    }

    if (match(TokenType::INTEGER)) {
        auto result = makeNode<Literal>();
        try {
            result->value =
                static_cast<std::int64_t>(std::stoll(previous().lexeme));
        } catch (const std::exception&) {
            error(previous(), "Invalid integer literal");
        }
        return result;
    }

    if (match(TokenType::DECIMAL)) {
        auto result = makeNode<Literal>();
        try {
            result->value = std::stod(previous().lexeme);
        } catch (const std::exception&) {
            error(previous(), "Invalid decimal literal");
        }
        return result;
    }

    if (match(TokenType::STRING)) {
        const std::string& lexeme = previous().lexeme;

        if (lexeme.size() < 2) {
            error(previous(), "Invalid string literal");
        }

        auto result = makeNode<Literal>();
        result->value = decodeEscapes(
            lexeme.substr(1, lexeme.size() - 2)
        );
        return result;
    }

    if (match(TokenType::INTERPOLATED_STRING)) {
        return parseInterpolatedString(previous().lexeme);
    }

    if (match(TokenType::LEFT_BRACKET)) {
        return parseArrayLiteral();
    }

    if (match(TokenType::LEFT_BRACE)) {
        return parseMapLiteral();
    }

    if (match(TokenType::TRUE)) {
        auto result = makeNode<Literal>();
        result->value = true;
        return result;
    }

    if (match(TokenType::FALSE)) {
        auto result = makeNode<Literal>();
        result->value = false;
        return result;
    }

    if (match(TokenType::DOLLAR)) {
        std::string name =
            expect(TokenType::IDENTIFIER).lexeme;

        while (match(TokenType::DOT)) {
            name += '.';
            name += expect(TokenType::IDENTIFIER).lexeme;
        }

        auto result = makeNode<VariableAccess>();
        result->name = std::move(name);
        return result;
    }

    if (match(TokenType::HASH)) {
        std::string name =
            expect(TokenType::IDENTIFIER).lexeme;

        while (match(TokenType::DOT)) {
            name += '.';
            name += expect(TokenType::IDENTIFIER).lexeme;
        }

        auto result = makeNode<VariableReference>();
        result->name = std::move(name);
        return result;
    }

    if (match(TokenType::LEFT_PAREN)) {
        auto expression = parseExpression();
        expect(TokenType::RIGHT_PAREN);
        return expression;
    }

    if (check(TokenType::IDENTIFIER)) {
        return parseFunctionCallOrVariable();
    }

    error(peek(), "Unexpected token in expression");
}

// ------------------------------------------------------------
// Calls, arrays, maps and ranges
// ------------------------------------------------------------

ExprPtr Parser::parseFunctionCallOrVariable() {
    std::string name =
        expect(TokenType::IDENTIFIER).lexeme;

    while (match(TokenType::DOT)) {
        name += '.';
        name += expect(TokenType::IDENTIFIER).lexeme;
    }

    std::vector<ExprPtr> arguments;
    std::map<std::string, ExprPtr> optionalArguments;

    if (!check(TokenType::LEFT_BRACE) &&
        !check(TokenType::NEWLINE) &&
        !check(TokenType::DEDENT) &&
        !isAtEnd() &&
        !check(TokenType::RIGHT_PAREN) &&
        !check(TokenType::RIGHT_BRACKET) &&
        !check(TokenType::COMMA) &&
        canStartExpression()) {

        do {
            arguments.push_back(parseOr());
        } while (canContinueArguments());
    }

    if (match(TokenType::LEFT_BRACE)) {
        if (!check(TokenType::RIGHT_BRACE)) {
            do {
                const std::string key =
                    expect(TokenType::IDENTIFIER).lexeme;

                expect(TokenType::EQUAL);
                optionalArguments[key] = parseExpression();
            } while (match(TokenType::COMMA));
        }

        expect(TokenType::RIGHT_BRACE);
    }

    auto result = makeNode<FunctionCall>();
    result->fullyQualifiedName = std::move(name);
    result->arguments = std::move(arguments);
    result->optionalArguments = std::move(optionalArguments);
    return result;
}

ExprPtr Parser::parseArrayLiteral() {
    std::vector<ExprPtr> elements;

    if (!check(TokenType::RIGHT_BRACKET)) {
        do {
            elements.push_back(parseExpression());
        } while (match(TokenType::COMMA));
    }

    expect(TokenType::RIGHT_BRACKET);

    auto result = makeNode<ArrayLiteral>();
    result->elements = std::move(elements);
    return result;
}

ExprPtr Parser::parseMapLiteral() {
    std::map<std::string, ExprPtr> entries;

    if (!check(TokenType::RIGHT_BRACE)) {
        do {
            std::string key =
                expect(TokenType::STRING).lexeme;

            if (key.size() < 2) {
                error(previous(), "Invalid map key");
            }

            key = decodeEscapes(key.substr(1, key.size() - 2));

            expect(TokenType::EQUAL);
            entries[key] = parseExpression();
        } while (match(TokenType::COMMA));
    }

    expect(TokenType::RIGHT_BRACE);

    auto result = makeNode<MapLiteral>();
    result->entries = std::move(entries);
    return result;
}

RangeLiteral Parser::parseRange() {
    const int start = std::stoi(
        expect(TokenType::INTEGER).lexeme
    );

    expect(TokenType::MINUS);

    const int end = std::stoi(
        expect(TokenType::INTEGER).lexeme
    );

    return RangeLiteral{start, end};
}

// ------------------------------------------------------------
// Interpolated strings
// ------------------------------------------------------------

ExprPtr Parser::parseInterpolatedString(const std::string& literal) const {
    if (literal.size() < 3) {
        error(previous(), "Invalid interpolated string");
    }

    // Erwartetes Tokenformat: $"...".
    const std::string content =
        literal.substr(2, literal.size() - 3);

    std::vector<StringPart> parts;
    std::string currentText;

    auto flushText = [&]() {
        if (currentText.empty()) return;

        StringPart part;
        part.isExpression = false;
        part.text = decodeEscapes(currentText);
        parts.push_back(std::move(part));
        currentText.clear();
    };

    std::size_t i = 0;

    while (i < content.size()) {
        if (content[i] != '$' || i + 1 >= content.size()) {
            currentText += content[i++];
            continue;
        }

        const std::size_t dollar = i;
        const char next = content[i + 1];

        if (next == '(') {
            flushText();
            i += 2;

            std::string expressionText;
            int depth = 1;
            bool inString = false;
            bool escaped = false;

            while (i < content.size() && depth > 0) {
                const char c = content[i++];

                if (escaped) {
                    expressionText += c;
                    escaped = false;
                    continue;
                }

                if (c == '\\' && inString) {
                    expressionText += c;
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inString = !inString;
                    expressionText += c;
                    continue;
                }

                if (!inString && c == '(') {
                    ++depth;
                } else if (!inString && c == ')') {
                    --depth;
                    if (depth == 0) break;
                }

                expressionText += c;
            }

            if (depth != 0) {
                error(previous(), "Unclosed interpolation expression");
            }

            Lexer lexer(expressionText);
            Parser parser(lexer.scanTokens());

            StringPart part;
            part.isExpression = true;
            part.expression = parser.parseExpressionOnly();
            parts.push_back(std::move(part));
            continue;
        }

        const auto isNameStart = [](const unsigned char c) {
            return std::isalpha(c) || c == '_';
        };

        const auto isNameChar = [](const unsigned char c) {
            return std::isalnum(c) || c == '_' || c == '.';
        };

        if (isNameStart(static_cast<unsigned char>(next))) {
            flushText();
            i += 1;

            std::string name;

            while (i < content.size() &&
                   isNameChar(static_cast<unsigned char>(content[i]))) {
                name += content[i++];
            }

            StringPart part;
            part.isExpression = true;

            auto variable = makeNode<VariableAccess>();
            variable->name = std::move(name);
            part.expression = std::move(variable);

            parts.push_back(std::move(part));
            continue;
        }

        // Ein Dollarzeichen ohne gültigen Interpolationsausdruck
        // bleibt gewöhnlicher Stringtext.
        currentText += content[dollar];
        i = dollar + 1;
    }

    flushText();

    auto result = makeNode<InterpolatedString>();
    result->parts = std::move(parts);
    return result;
}

// ------------------------------------------------------------
// Token utilities
// ------------------------------------------------------------

bool Parser::match(const TokenType type) {
    if (!check(type)) return false;
    advance();
    return true;
}

bool Parser::match(const std::initializer_list<TokenType> types) {
    for (const TokenType type : types) {
        if (check(type)) {
            advance();
            return true;
        }
    }

    return false;
}

bool Parser::check(const TokenType type) const {
    if (isAtEnd()) return false;
    return peek().type == type;
}

Token Parser::advance() {
    if (!isAtEnd()) ++current;
    return previous();
}

bool Parser::isAtEnd() const {
    return peek().type == TokenType::EOF_;
}

const Token& Parser::peek() const {
    return tokens.at(current);
}

const Token& Parser::previous() const {
    return tokens.at(current - 1);
}

const Token& Parser::consume(
    const TokenType type,
    const std::string& message
) {
    if (check(type)) {
        advance();
        return previous();
    }

    error(peek(), message);
}

const Token& Parser::expect(const TokenType type) {
    if (check(type)) {
        advance();
        return previous();
    }

    error(peek(), "Unexpected token; expected a different token type");
}

void Parser::skipNewlines() {
    while (match(TokenType::NEWLINE)) {}
}

bool Parser::canStartExpression() const {
    return check(TokenType::DOLLAR) ||
           check(TokenType::HASH) ||
           check(TokenType::INTEGER) ||
           check(TokenType::DECIMAL) ||
           check(TokenType::STRING) ||
           check(TokenType::INTERPOLATED_STRING) ||
           check(TokenType::LEFT_BRACKET) ||
           check(TokenType::LEFT_BRACE) ||
           check(TokenType::LEFT_PAREN) ||
           check(TokenType::IDENTIFIER) ||
           check(TokenType::NOT) ||
           check(TokenType::MINUS) ||
           check(TokenType::NULL_) ||
           check(TokenType::TRUE) ||
           check(TokenType::FALSE);
}

bool Parser::canContinueArguments() const {
    if (check(TokenType::LEFT_BRACE) ||
        check(TokenType::NEWLINE) ||
        check(TokenType::DEDENT) ||
        isAtEnd() ||
        check(TokenType::RIGHT_PAREN) ||
        check(TokenType::RIGHT_BRACKET) ||
        check(TokenType::COMMA)) {
        return false;
    }

    return canStartExpression();
}

[[noreturn]] void Parser::error(
    const Token& token,
    const std::string& message
) {
    throw std::runtime_error(
        message + " at " +
        std::to_string(token.line) + ":" +
        std::to_string(token.column)
    );
}
