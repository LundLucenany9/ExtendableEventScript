package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

import java.util.List;

public record ArrayLiteral(List<ExpressionNode> elements) implements ExpressionNode {
}
