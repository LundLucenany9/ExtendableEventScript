package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

public record RangeLiteral(int start, int end) implements ExpressionNode {
}
