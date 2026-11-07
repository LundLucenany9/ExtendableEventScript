package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

import java.util.List;

public record ArrayLiteral(List<ExpressionNode> elements) implements ExpressionNode {
}
