package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

public record Literal(Object value) implements ExpressionNode {
}
