package de.lundlucenany9.ees.ast;

import de.lundlucenany9.ees.ast.statements.ImportStatement;

import java.util.List;

public class Program implements AstNode{
    List<ImportStatement> imports;
    List<Statement> statements;
    public Program(List<ImportStatement> imports, List<Statement> statements) {
        this.imports = imports;
        this.statements = statements;
    }
    public List<ImportStatement> getImports() {
        return imports;
    }
    public List<Statement> getStatements() {
        return statements;
    }
}
