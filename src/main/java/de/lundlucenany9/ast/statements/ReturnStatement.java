package de.lundlucenany9.ast.statements;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.Statement;

public record ReturnStatement(ExpressionNode value) implements Statement {
}
