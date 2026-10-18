package de.lundlucenany9.runtime.simple;

import de.lundlucenany9.ast.expressions.VariableReference;
import de.lundlucenany9.runtime.ScriptException;
import de.lundlucenany9.runtime.Variable;

import java.util.List;
import java.util.Map;

public class StandardMethods implements Library{
    @Override
    public String getName() {
        return "std";
    }

    @EESFunction
    public void log(Object message, @EESOptional(name = "newline", type = EESOptional.Type.BOOLEAN, booleanValue = true) boolean newline) {
        if(newline) message += "\n";
        System.out.print(message);
    }
    @EESFunction
    public void err(Object message, @EESOptional(name = "throw", type = EESOptional.Type.BOOLEAN) boolean throwException) {
        if(throwException) throw new RuntimeException(message.toString());
        System.err.println(message);
    }

    @EESFunction(name = "string")
    public String string_(Object object) {
        return object.toString();
    }

    @EESFunction(name = "int")
    public int int_(Object object) {
        return Integer.parseInt(object.toString());
    }

    @EESFunction(name = "double")
    public double double_(Object object) {
        return Double.parseDouble(object.toString());
    }

    @EESFunction
    public boolean bool(Object object) {
        return Boolean.parseBoolean(object.toString());
    }

    @EESFunction
    public int len(Object object) {
        if(object instanceof String) return ((String) object).length();
        if(object instanceof List) return ((List<?>) object).size();
        if(object instanceof Map) return ((Map<?, ?>) object).size();
        throw new ScriptException("Cannot get length of " + object.getClass().getName());
    }

}
