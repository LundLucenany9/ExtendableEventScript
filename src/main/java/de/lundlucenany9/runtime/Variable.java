package de.lundlucenany9.runtime;

public class Variable {

    private static final Variable UNDEFINED = new Variable(true, StaticObjects.UNDEFINED);

    private boolean isDefined = false;
    private Object value;
    private final boolean isReadOnly;

    private Variable(boolean isReadOnly, Object value) {
        this.isDefined = value != StaticObjects.UNDEFINED;
        this.value = value;
        this.isReadOnly = isReadOnly;
    }

    public static Variable define(Object value) {
        return new Variable(false, value);
    }
    public static Variable define(Object value, boolean isReadOnly) {
        return new Variable(isReadOnly, value);
    }
    public static Variable undefined() {
        return UNDEFINED;
    }
    public static Variable readOnly(Object value) {
        return new Variable(true, value);
    }

    public boolean isDefined() {
        return isDefined;
    }
    public Object value() {
        //if(!isDefined) throw new ScriptException("Variable is not defined");
        return value;
    }

    public Variable assign(Object value) {
        if(!isDefined) throw new ScriptException("Variable is not defined");
        if(isReadOnly) throw new ScriptException("Variable is Read Only");
        if(value == StaticObjects.UNDEFINED) throw new ScriptException("Can not assign undefined to variable");
        return set(value);
    }

    public Variable set(Object value) {
        if(!isDefined) throw new ScriptException("Variable is not defined");
        this.value = value;
        this.isDefined = value != StaticObjects.UNDEFINED;
        return this;
    }
    public Variable undefine() {
        this.isDefined = false;
        this.value = StaticObjects.UNDEFINED;
        return this;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }
}
