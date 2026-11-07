package de.lundlucenany9.ees.ast.statements;

import de.lundlucenany9.ees.ast.ExpressionNode;
import de.lundlucenany9.ees.ast.Statement;

import java.util.List;

public record ForEachLoop(String indexVar, String itemVar, ExpressionNode iterable, List<Statement> body) implements Statement {
}
