package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;

import java.util.List;
import java.util.Map;

public record FunctionCall(String fullyQualifiedName, List<ExpressionNode> arguments, Map<String, ExpressionNode> optionalArguments) implements ExpressionNode {
}
