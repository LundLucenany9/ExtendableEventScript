package de.lundlucenany9.ees.ast.statements;

import de.lundlucenany9.ees.ast.ExpressionNode;
import de.lundlucenany9.ees.ast.Statement;

public record ExpressionStatement(ExpressionNode expression) implements Statement {
}
