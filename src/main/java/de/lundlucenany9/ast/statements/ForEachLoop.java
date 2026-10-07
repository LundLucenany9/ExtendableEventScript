package de.lundlucenany9.ast.statements;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.Statement;

import java.util.List;

public record ForEachLoop(String indexVar, String itemVar, ExpressionNode iterable, List<Statement> body) implements Statement {
}
