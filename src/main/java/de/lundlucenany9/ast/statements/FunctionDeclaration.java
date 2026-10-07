package de.lundlucenany9.ast.statements;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.FunctionParameter;
import de.lundlucenany9.ast.Statement;

import java.util.List;
import java.util.Map;

public record FunctionDeclaration(String name, List<FunctionParameter> parameters, Map<String, ExpressionNode> optionalParams, List<Statement> body, boolean isGlobal) implements Statement{
}
