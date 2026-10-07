package de.lundlucenany9.ast.expressions;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.Statement;

public record IndexedAssignment(ExpressionNode target, ExpressionNode index, ExpressionNode value) implements ExpressionNode {
}
