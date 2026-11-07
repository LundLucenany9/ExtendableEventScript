package de.lundlucenany9.ees.ast.statements;

import de.lundlucenany9.ees.ast.Statement;
import de.lundlucenany9.ees.ast.expressions.RangeLiteral;

import java.util.List;

public record ForLoop(String variable, RangeLiteral range, List<Statement> body) implements Statement {
}
