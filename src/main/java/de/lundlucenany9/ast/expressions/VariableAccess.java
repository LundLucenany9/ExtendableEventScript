package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

public record VariableAccess(String name) implements ExpressionNode {
}
