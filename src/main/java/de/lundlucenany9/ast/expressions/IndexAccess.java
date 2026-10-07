package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

public record IndexAccess(ExpressionNode target, ExpressionNode index) implements ExpressionNode {
}
