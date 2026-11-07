package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

public record Literal(Object value) implements ExpressionNode {
}
