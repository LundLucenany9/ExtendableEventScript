package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.Statement;

public record Assignment(String variableName, ExpressionNode value) implements ExpressionNode {
}
