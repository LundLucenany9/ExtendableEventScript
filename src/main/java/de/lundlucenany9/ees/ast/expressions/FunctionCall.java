package de.lundlucenany9.ees.ast.expressions;

import de.lundlucenany9.ees.ast.ExpressionNode;

import java.util.List;
import java.util.Map;

public record FunctionCall(String fullyQualifiedName, List<ExpressionNode> arguments, Map<String, ExpressionNode> optionalArguments) implements ExpressionNode {
}
