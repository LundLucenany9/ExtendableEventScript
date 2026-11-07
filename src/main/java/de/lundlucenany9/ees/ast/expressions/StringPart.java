package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

public record StringPart(boolean isExpression, String text, ExpressionNode expression) implements ExpressionNode {
}
