package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

public record IndexedAssignment(ExpressionNode target, ExpressionNode index, ExpressionNode value) implements ExpressionNode {
}
