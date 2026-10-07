package de.lundlucenany9.ast.statements;

import de.lundlucenany9.ast.Statement;

public record ImportStatement(String module) implements Statement {
}
