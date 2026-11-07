package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

import java.util.List;

public record InterpolatedString(List<StringPart> parts) implements ExpressionNode {
}
