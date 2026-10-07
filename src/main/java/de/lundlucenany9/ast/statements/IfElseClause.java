package de.lundlucenany9.ast.statements;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.Statement;

import java.util.List;

public record IfElseClause(ExpressionNode condition, List<Statement> body) implements Statement {
}
