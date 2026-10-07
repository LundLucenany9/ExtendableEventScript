package de.lundlucenany9.ast.statements;

import de.lundlucenany9.ast.Statement;
import de.lundlucenany9.ast.expressions.RangeLiteral;

import java.util.List;

public record ForLoop(String variable, RangeLiteral range, List<Statement> body) implements Statement {
}
