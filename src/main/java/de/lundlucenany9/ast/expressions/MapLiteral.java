package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

import java.util.Map;

public record MapLiteral(Map<String, ExpressionNode> entries) implements ExpressionNode {
}
