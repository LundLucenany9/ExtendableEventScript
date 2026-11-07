package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

import java.util.Map;

public record MapLiteral(Map<String, ExpressionNode> entries) implements ExpressionNode {
}
