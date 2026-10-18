package de.lundlucenany9.runtime;

import de.lundlucenany9.ast.FunctionParameter;

import java.util.List;
import java.util.Map;

public record Function(List<FunctionParameter> parameters, Map<String, Object> optionals, byte[] code, int bodyStart, Frame definingFrame) {
}
