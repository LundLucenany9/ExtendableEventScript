package de.lundlucenany9.ast.statements;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.Statement;

public record VarDeclaration(String name, ExpressionNode initializer, boolean isReadOnly) implements Statement {
}
