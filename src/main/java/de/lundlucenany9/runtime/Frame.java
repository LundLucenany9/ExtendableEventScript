package de.lundlucenany9.runtime;


import de.lundlucenany9.debug.DebugVisualizer;

import java.util.*;

public final class Frame {
    private final Deque<Object> stack;
    private final Map<String, Variable> locals;
    private final Set<String> tmp;
    private final Map<String, Function> functions = new HashMap<>();
    private final Map<String, Variable> event;
    private final Map<String, Variable> args;

    private static int frames = 0;
    private int frameId = frames++;

    private final Frame parent;

    public Frame(Map<String, Variable> event, Map<String, Variable> args,Frame parent) {
        stack = new ArrayDeque<>();
        locals = new HashMap<>();
        tmp = new HashSet<>();
        this.event = event;
        this.args = args;
        this.parent = parent;
    }

    public void push(Object value) {
        //DebugVisualizer.draw("[" + frameId + "] " +  stack.size() + "  Pushing " + value + " to stack");
        stack.push(value);
        //DebugVisualizer.draw("[" + frameId + "] " + stack.size() + "  Stack now: " + stack);
    }

    public Object pop() {
        //DebugVisualizer.draw("[" + frameId + "] " + stack.size() + "  Popping " + stack.peek() + " from stack");
        return stack.pop();
    }

    public Object peek() {
        return stack.peek();
    }

    public void define(String name, Variable variable) {
        locals.put(name, variable);
    }
    public Variable get(String name) {
        return locals.getOrDefault(name, Variable.undefined());
    }
    public boolean isDefined(String name) {
        return locals.containsKey(name);
    }
    public void undefine(String name) {
        locals.remove(name);
    }
    public boolean isTmp(String name) {
        return tmp.contains(name);
    }
    public void setTmp(String name) {
        tmp.add(name);
    }
    public void removeTmp(String name) {
        tmp.remove(name);
    }

    public void defineFunction(String name, Function function) {
        if(functions.containsKey(name)) throw new ScriptException("Function with name " + name + " already exists");
        functions.put(name, function);
    }
    public Function getFunction(String name) {
        return functions.get(name);
    }
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    public Map<String, Variable> getEvent() {
        if(parent != null) return parent.getEvent();
        return event;
    }
    public Map<String, Variable> getArgs() {
        if(parent != null) return parent.getArgs();
        return args;
    }

    public Frame getParent() {
        return parent;
    }
    public List<Object> getStack() {
        return Collections.unmodifiableList(new ArrayList<>(stack));
    }
}
