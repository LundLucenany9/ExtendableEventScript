
#pragma once

#include "ees/TokenType.h"

#include <cstdint>
#include <map>
#include <memory>
#include <string>
#include <variant>
#include <vector>

namespace ees::ast {

// Base types

struct AstNode {
    virtual ~AstNode() = default;
};

struct ExpressionNode : AstNode {
    ~ExpressionNode() override = default;
};

struct Statement : AstNode {
    ~Statement() override = default;
};

using ExprPtr = std::unique_ptr<ExpressionNode>;
using StmtPtr = std::unique_ptr<Statement>;

// Function parameters

struct FunctionParameter {
    std::string name;
    bool isReference;
};

// Expressions

struct ArrayLiteral : ExpressionNode {
    std::vector<ExprPtr> elements;
};

struct Assignment : ExpressionNode {
    std::string variableName;
    ExprPtr value;
};

struct BinaryOp : ExpressionNode {
    ExprPtr left;
    TokenType operatorType;
    ExprPtr right;
};

struct FunctionCall : ExpressionNode {
    std::string fullyQualifiedName;
    std::vector<ExprPtr> arguments;
    std::map<std::string, ExprPtr> optionalArguments;
};

struct IndexAccess : ExpressionNode {
    ExprPtr target;
    ExprPtr index;
};

struct IndexedAssignment : ExpressionNode {
    ExprPtr target;
    ExprPtr index;
    ExprPtr value;
};

struct StringPart {
    bool isExpression;
    std::string text;
    ExprPtr expression;
};

struct InterpolatedString : ExpressionNode {
    std::vector<StringPart> parts;
};

using LiteralValue = std::variant<
    //std::monostate, // undefined not in ast
    std::nullptr_t, // null
    bool,
    std::int64_t,
    double,
    std::string
>;

struct Literal : ExpressionNode {
    LiteralValue value;
};

struct MapLiteral : ExpressionNode {
    std::map<std::string, ExprPtr> entries;
};

    struct RangeLiteral : ExpressionNode {
        int start{};
        int end{};

        RangeLiteral() = default;
        RangeLiteral(const int start, const int end) : start(start), end(end) {}
    };

struct UnaryOp : ExpressionNode {
    TokenType operatorType;
    ExprPtr operand;
};

struct VariableAccess : ExpressionNode {
    std::string name;
};

struct VariableReference : ExpressionNode {
    std::string name;
};

// Statements

struct ExpressionStatement : Statement {
    ExprPtr expression;
};

struct ForEachLoop : Statement {
    std::string indexVar;
    std::string itemVar;
    ExprPtr iterable;
    std::vector<StmtPtr> body;
};

struct ForLoop : Statement {
    std::string variable;
    RangeLiteral range;
    std::vector<StmtPtr> body;
};

struct FunctionDeclaration : Statement {
    std::string name;
    std::vector<FunctionParameter> parameters;
    std::map<std::string, ExprPtr> optionalParams;
    std::vector<StmtPtr> body;
    bool isGlobal;
};

struct IfElseClause {
    ExprPtr condition;
    std::vector<StmtPtr> body;
};

struct IfStatement : Statement {
    ExprPtr condition;
    std::vector<StmtPtr> thenBody;
    std::vector<IfElseClause> elifClauses;
    std::vector<StmtPtr> elseBody;
};

struct ImportStatement : Statement {
    std::string module;
};

struct ReturnStatement : Statement {
    ExprPtr value; // nullptr entspricht einem return ohne Wert
};

struct VarDeclaration : Statement {
    std::string name;
    ExprPtr initializer;
    bool isReadOnly;
};

// Program

struct Program : AstNode {
    std::vector<std::unique_ptr<ImportStatement>> imports;
    std::vector<StmtPtr> statements;
};

} // namespace ees::ast
