package de.lundlucenany9.runtime.simple;

import de.lundlucenany9.ast.expressions.VariableReference;
import de.lundlucenany9.runtime.ScriptException;
import de.lundlucenany9.runtime.Variable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StandardMethods implements Library{
    @Override
    public String getName() {
        return "std";
    }

    @EESFunction
    public void log(Object message, @EESOptional(name = "newline") Optional<Boolean> newline) {
        boolean nl = newline.orElse(true);
        if(nl) message += "\n";
        System.out.print(message);
    }

    @EESFunction
    public void logf(String format, List<Object> args) {
        System.out.printf(format, args.toArray());
    }

    @EESFunction
    public void err(Object message, @EESOptional(name = "throw") Optional<Boolean> throwException) {
        boolean throwException_ = throwException.orElse(false);
        if(throwException_) throw new RuntimeException(message.toString());
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

    @EESFunction
    public int max(int a, int b) {
        return Math.max(a, b);
    }

    @EESFunction
    public int min(int a, int b) {
        return Math.min(a, b);
    }

    @EESFunction
    public int round(double x) {
        return (int) Math.round(x);
    }

    @EESFunction
    public double abs(double x) {
        return Math.abs(x);
    }

}
