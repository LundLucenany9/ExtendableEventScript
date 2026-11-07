package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

public record IndexAccess(ExpressionNode target, ExpressionNode index) implements ExpressionNode {
}
