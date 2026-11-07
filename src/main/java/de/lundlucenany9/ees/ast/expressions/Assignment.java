package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

public record Assignment(String variableName, ExpressionNode value) implements ExpressionNode {
}
