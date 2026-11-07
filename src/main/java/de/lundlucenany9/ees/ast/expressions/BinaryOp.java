package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;
import de.lundlucenany9.ees.TokenType;

public record BinaryOp(ExpressionNode left, TokenType operator, ExpressionNode right) implements ExpressionNode {
}
