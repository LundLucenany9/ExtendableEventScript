package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.TokenType;
import de.lundlucenany9.ast.ExpressionNode;

public record UnaryOp(TokenType operator, ExpressionNode operand) implements ExpressionNode {
}
