package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

import java.util.List;

public record InterpolatedString(List<StringPart> parts) implements ExpressionNode {
}
