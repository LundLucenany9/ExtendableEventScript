package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

public record StringPart(boolean isExpression, String text, ExpressionNode expression) implements ExpressionNode {
}
