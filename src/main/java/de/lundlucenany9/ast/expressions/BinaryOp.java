package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.TokenType;

public record BinaryOp(ExpressionNode left, TokenType operator, ExpressionNode right) implements ExpressionNode {
}
