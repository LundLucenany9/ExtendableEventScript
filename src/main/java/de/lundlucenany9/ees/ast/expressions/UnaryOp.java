package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.TokenType;
import de.lundlucenany9.ees.ast.ExpressionNode;

public record UnaryOp(TokenType operator, ExpressionNode operand) implements ExpressionNode {
}
