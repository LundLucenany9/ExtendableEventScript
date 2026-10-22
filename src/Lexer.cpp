//
// Created by Admin on 22.10.2026.
//

#include <utility>

#include "ees/Lexer.h"
#include <cctype>
#include <stdexcept>
#include <unordered_map>

Lexer::Lexer(std::string  source) : source(std::move(source.replace(source.begin(), source.end(), "\r\n", "\n"))) {}

bool Lexer::isAtEnd() const {
    return current >= source.size();
}

char Lexer::advance() {
    const char c = source[current++];

    if (c == '\n') {
        ++line;
        column = 1;
    } else {
        ++column;
    }
    return c;
}

char Lexer::peek() const {
    if (isAtEnd()) return '\0';

    return source[current];
}

char Lexer::peekNext() const {
    if (current + 1 >= source.size()) return '\0';

    return source[current + 1];
}

bool Lexer::match(const char expected) {
    if (isAtEnd() || source[current] != expected) return false;

    advance();
    return true;
}

void Lexer::addToken(TokenType type) {
    tokens.emplace_back(
        type,
        source.substr(start, current - start),
        line,
        column
    );
}

void Lexer::addIndent() {
    tokens.emplace_back(
        TokenType::INDENT,
        "",
        line,
        column
    );
}

void Lexer::addDedent() {
    tokens.emplace_back(
        TokenType::DEDENT,
        "",
        line,
        column
    );
}

void Lexer::skipWhitespace() {
    while (!isAtEnd()) {
        if (const char c = peek(); c == ' ' || c == '\t' || c == '\r') {
            advance();
        } else {
            break;
        }
    }
}

void Lexer::scanIdentifier() {
    while (std::isalnum(
            static_cast<unsigned char>(peek())
            || peek() == '_'
        )) {
        advance();
    }
    const std::string text = source.substr(start, current - start);

    static const std::unordered_map<std::string, TokenType> keywords = {
        {"import", TokenType::IMPORT},
        {"global", TokenType::GLOBAL},
        {"var", TokenType::VAR},
        {"rovar", TokenType::ROVAR},
        {"function", TokenType::FUNC},
        {"if", TokenType::IF},
        {"elif", TokenType::ELIF},
        {"else", TokenType::ELSE},
        {"for", TokenType::FOR},
        {"foreach", TokenType::FOREACH},
        {"return", TokenType::RETURN},
        {"null", TokenType::NULL_},
        {"true", TokenType::TRUE},
        {"false", TokenType::FALSE},
        {"and", TokenType::AND},
        {"or", TokenType::OR}
    };

    if (const auto it = keywords.find(text); it != keywords.end()) {
        addToken(it->second);
    } else {
        addToken(TokenType::IDENTIFIER);
    }
}

void Lexer::scanNumber() {
    while (std::isdigit(
            static_cast<unsigned char>(peek())
        )) {
        advance();
    }

    bool isFloat = false;

    if (peek() == '.' && std::isdigit(static_cast<unsigned char>(peekNext()))) {
        isFloat = true;
        advance();

        while (std::isdigit(static_cast<unsigned char>(peek())))
            advance();
    }
    addToken(isFloat ? TokenType::DECIMAL : TokenType::INTEGER);
}

void Lexer::scanString() {
    while (!isAtEnd() && peek() != '"') {
        if (peek() == '\n') {
            throw std::runtime_error("Unterminated string at line " + std::to_string(line));
        }

        if (peek() == '\\') {
            advance();

            if (!isAtEnd())
                advance();
        } else
            advance();
    }

    if (isAtEnd())
        throw std::runtime_error("Unterminated string at line " + std::to_string(line));

    advance();

}

void Lexer::scanToken() {
    switch (const char c = advance()) {
        case ' ':
        case '\r':
            break;

        case '\n': {
            addToken(TokenType::NEWLINE);

            int indentation = 0;

            while (peek() == ' ') {
                advance();
                indentation++;
            }

            // Leerzeile: diese Zeile darf den Indent-Level nicht verändern.
            if (peek() == '\n' || isAtEnd()) {
                break;
            }

            const int previous = indents.back();
            indents.pop_back();

            if (indentation > previous) {
                indents.push_back(indentation);
                addIndent();
            } else if (indentation < previous) {
                while (indentation < indents.back()) {
                    indents.pop_back();
                    addDedent();
                }

                if (indentation != indents.back()) {
                    throw std::runtime_error("Invalid indentation on line " + std::to_string(line));
                }
            }

            break;
        }

        case '/':
            if (match('/'))
                while (peek() != '\n' && !isAtEnd())
                    advance();
            else
                addToken(TokenType::SLASH);
            break;

        case '"':
            scanString();
            addToken(TokenType::STRING);
            break;

        case '-':
            addToken(match('>') ? TokenType::ARROW : TokenType::MINUS);
            break;

        case '+': addToken(TokenType::PLUS); break;
        case '*': addToken(TokenType::STAR); break;
        case '$':
            if (match('"')) {
                scanString();
                addToken(TokenType::INTERPOLATED_STRING);
            }
            else
                addToken(TokenType::DOLLAR);
            break;
        case '#': addToken(TokenType::HASH); break;
        case '.': addToken(TokenType::DOT); break;
        case ',': addToken(TokenType::COMMA); break;

        case '=': addToken(match('=') ? TokenType::EQUAL_EQUAL : TokenType::EQUAL); break;

        case '!':
            if (match('='))
                addToken(TokenType::NOT_EQUAL);
            else
                addToken(TokenType::NOT);
            break;

        case '<':
            addToken(match('=') ? TokenType::LESS_EQUAL : TokenType::LESS);
            break;

        case '>':
            addToken(match('=') ? TokenType::GREATER_EQUAL : TokenType::GREATER);
            break;

        default:
            if (std::isdigit(c))
                scanNumber();
            else if (isIdentifierStart(c))
                scanIdentifier();
            else {
                std::string err = "Unexpected character: ";
                err += c;
                throw std::runtime_error(err);
            }
    }
}

bool Lexer::isIdentifierStart(const char c) {
    return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || c == '_';
}

std::vector<Token> Lexer::scanTokens() {
    while (current < source.length())
        scanToken();
    while (indents.size() > 1) {
        indents.pop_back();
        addDedent();
    }

    start = current;
    addToken(TokenType::EOF_);
    return tokens;
}
