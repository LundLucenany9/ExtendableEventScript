package de.lundlucenany9.ees.ast.statements;

import de.lundlucenany9.ees.ast.ExpressionNode;
import de.lundlucenany9.ees.ast.Statement;

public record VarDeclaration(String name, ExpressionNode initializer, boolean isReadOnly) implements Statement {
}
