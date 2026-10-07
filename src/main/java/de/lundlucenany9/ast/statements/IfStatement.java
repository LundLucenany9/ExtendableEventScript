package de.lundlucenany9.ast.statements;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.Statement;

import java.util.List;

public record IfStatement(ExpressionNode condition, List<Statement> thenBody, List<IfElseClause> elifClauses, List<Statement> elseBody) implements Statement {
}
