package de.lundlucenany9.ees.ast.statements;

import de.lundlucenany9.ees.ast.ExpressionNode;
import de.lundlucenany9.ees.ast.FunctionParameter;
import de.lundlucenany9.ees.ast.Statement;

import java.util.List;
import java.util.Map;

public record FunctionDeclaration(String name, List<FunctionParameter> parameters, Map<String, ExpressionNode> optionalParams, List<Statement> body, boolean isGlobal) implements Statement{
}
