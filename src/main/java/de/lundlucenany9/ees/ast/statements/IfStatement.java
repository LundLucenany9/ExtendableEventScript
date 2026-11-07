package de.lundlucenany9.ees.ast.statements;

import de.lundlucenany9.ees.ast.ExpressionNode;
import de.lundlucenany9.ees.ast.Statement;

import java.util.List;

public record IfStatement(ExpressionNode condition, List<Statement> thenBody, List<IfElseClause> elifClauses, List<Statement> elseBody) implements Statement {
}
