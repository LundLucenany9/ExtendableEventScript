package de.lundlucenany9.runtime;

import de.lundlucenany9.ast.FunctionParameter;
import de.lundlucenany9.compiler.Opcode;
import de.lundlucenany9.compiler.PushType;
import de.lundlucenany9.debug.DebugVisualizer;

import java.nio.ByteBuffer;
import java.util.*;

public class VM {
    private static final Opcode[] opcodes = Opcode.values();

    private final Host host;
    private final Map<String, Variable> globals;


    private final Map<String, Function> globalFunctions;
    private final Deque<Integer> stack = new ArrayDeque<>();
    
    public VM(Host host) {
        this.host = host;
        this.globals = new HashMap<>();
        this.globalFunctions = new HashMap<>();
    }

    public void run(byte[] bytecode, Map<String, Variable> args, Map<String, Variable> event) {
        ByteBuffer buf = ByteBuffer.wrap(bytecode);
        buf.position(5);
        Frame f = new Frame(event, args, null);
        
        while(buf.hasRemaining() && !Thread.currentThread().isInterrupted()) {
            Opcode op = opcodes[buf.get()];
            //System.out.println("op = " + op);
            switch (op) {
                case PUSH -> f.push(readPushValue(buf, f));
                case POP -> {
                    try {
                        f.pop();
                    }catch (NoSuchElementException e) {
                        DebugVisualizer.draw("Interrupted while popping from stack");
                        Thread.currentThread().interrupt();
                    }
                }
                case DEFINE_TMP -> {String n = readString(buf); f.define(n, Variable.readOnly(null)); f.setTmp(n);}
                case UNDEFINE_TMP -> {String n = readString(buf); f.undefine(n); f.removeTmp(n);}
                case LOAD -> f.push(resolve(f, readString(buf)));
                case LOAD_REF -> f.push(resolveRef(f, readString(buf)));
                case STORE -> store(f, readString(buf), f.pop());
                case STORE_TMP -> storeTMP(f, readString(buf), f.pop());
                case ADD -> add(f, f.pop(), f.pop());
                case SUB -> sub(f, f.pop(), f.pop());
                case MUL -> mul(f, f.pop(), f.pop());
                case DIV -> div(f, f.pop(), f.pop());
                case LTE -> {Object b = f.pop(); Object a = f.pop(); f.push(a instanceof Number && b instanceof Number && ((Number)a).doubleValue() <= ((Number)b).doubleValue());}
                case LT -> {Object b = f.pop(); Object a = f.pop(); f.push(a instanceof Number && b instanceof Number && ((Number)a).doubleValue() < ((Number)b).doubleValue());}
                case GTE -> {Object b = f.pop(); Object a = f.pop(); f.push(a instanceof Number && b instanceof Number && ((Number)a).doubleValue() >= ((Number)b).doubleValue());}
                case GT -> {Object b = f.pop(); Object a = f.pop(); f.push(a instanceof Number && b instanceof Number && ((Number)a).doubleValue() > ((Number)b).doubleValue());}
                case EQ -> {Object b = f.pop(); Object a = f.pop(); f.push(a.equals(b));}
                case NEQ -> {Object b = f.pop(); Object a = f.pop(); f.push(!a.equals(b));}
                case AND -> {Object b = f.pop(); Object a = f.pop(); f.push(truthy(a) && truthy(b));}
                case OR -> {Object b = f.pop(); Object a = f.pop(); f.push(truthy(a) || truthy(b));}
                case NOT -> f.push(!truthy(f.pop()));
                case JMP -> buf.position(buf.getInt());
                case JMP_IF_NOT -> {int t = buf.getInt(); if(!truthy(f.pop())) buf.position(t);}
                case JMP_IF -> {int t = buf.getInt(); if(truthy(f.pop())) buf.position(t);}
                case DUP -> f.push(f.peek());
                case RET -> {
                    Object ret = f.pop();
                    f = f.getParent();
                    f.push(ret);
                    //if(f == null) return;
                    buf.position(stack.pop());
                }
                case NEG -> {
                    Object o = f.pop();
                    if(o instanceof Number) {
                        if(o instanceof Integer) f.push(-((Integer) o));
                        else f.push(-((Number) o).doubleValue());
                    }
                    else throw new ScriptException("Cannot negate " + typeName(o));
                }
                case INDEX -> {
                    Object key = f.pop();
                    Object target = f.pop();

                    if (target instanceof List<?> list) {
                        if (!(key instanceof Integer idx))
                            throw new ScriptException("Cannot index list with " + typeName(key));
                        f.push(idx >= 0 && idx < list.size() ? list.get(idx) : StaticObjects.UNDEFINED);
                    } else if (target instanceof Map<?, ?> map) {
                        if (!(key instanceof String k))
                            throw new ScriptException("Cannot index map with " + typeName(key));
                        f.push(map.containsKey(k) ? map.get(k) : StaticObjects.UNDEFINED);
                    } else if (target instanceof String s) {
                        if (!(key instanceof Integer idx))
                            throw new ScriptException("Cannot index string with " + typeName(key));
                        f.push(idx >= 0 && idx < s.length() ? String.valueOf(s.charAt(idx)) : StaticObjects.UNDEFINED);
                    } else {
                        throw new ScriptException("Cannot index " + typeName(target));
                    }
                }
                case INDEX_ASSIGN -> {
                    Object target = f.pop();
                    Object index = f.pop();
                    Object value = f.pop();
                    try {
                        if (target instanceof List<?> rawList) {
                            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) rawList;
                            if (!(index instanceof Integer i))
                                throw new ScriptException("Cannot assign to list with " + typeName(index));
                            if (i < 0 || i > list.size())
                                throw new ScriptException("Index " + i + " out of range");
                            if (i == list.size()) list.add(value); else list.set(i, value);
                            f.push(value);
                        } else if (target instanceof Map<?, ?> rawMap) {
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) rawMap;
                            if (!(index instanceof String k))
                                throw new ScriptException("Cannot assign to map with " + typeName(index));
                            map.put(k, value);
                            f.push(value);
                        } else {
                            throw new ScriptException("Cannot assign to " + typeName(target));
                        }
                    } catch (UnsupportedOperationException e) {
                        throw new ScriptException("Cannot modify read-only " + typeName(target));
                    }
                }
                case DEFINE_VAR -> {
                    boolean ro = buf.get() == 1;
                    String name = readString(buf);
                    Object value = f.pop();
                    if(value == StaticObjects.UNDEFINED) throw new ScriptException("Cannot define undefined variable");
                    int dot = name.indexOf('.');
                    if (dot < 0) throw new ScriptException("Variable name without scope: " + name);
                    String scope = name.substring(0, dot), var = name.substring(dot + 1);
                    switch (scope) {
                        case "glob" -> globals.putIfAbsent(var, Variable.define(value, ro));
                        case "local", "func" -> {
                            if(f.isDefined(name)) throw new ScriptException("Variable " + name + " already defined");
                            f.define(name, Variable.define(value, ro));
                        }
                        default -> throw new ScriptException("Unknown variable scope: " + scope);
                    }
                }
                case DEFINE_FUNC -> {
                    String name = readString(buf);

                    int argc = buf.get() & 0xFF;
                    List<FunctionParameter> params = new ArrayList<>();
                    for (int i = 0; i < argc; i++) {
                        boolean isRef = buf.get() == 1;
                        params.add(new FunctionParameter(readString(buf), isRef));
                    }

                    int optc = buf.get() & 0xFF;
                    String[] optNames = new String[optc];
                    for (int i = 0; i < optc; i++) optNames[i] = readString(buf);
                    Object[] optValues = new Object[optc];
                    for (int i = optc - 1; i >= 0; i--) optValues[i] = f.pop();   // umgekehrt zum Pushen
                    Map<String, Object> defaults = new LinkedHashMap<>();
                    for (int i = 0; i < optc; i++) defaults.put(optNames[i], optValues[i]);

                    int end = buf.getInt();
                    int bodyStart = buf.position();
                    Function fn = new Function(params, defaults, bytecode, bodyStart, f);

                    int dot = name.indexOf('.');
                    String scope = name.substring(0, dot), fnName = name.substring(dot + 1);
                    switch (scope) {
                        case "glob"  -> globalFunctions.put(fnName, fn);
                        case "local" -> f.defineFunction(fnName, fn);
                        default      -> throw new ScriptException("Unknown function scope: " + scope);
                    }
                    buf.position(end);          // Body überspringen
                }
                case CALL -> {
                    String name = readString(buf);
                    int argc = buf.get(); int optc = buf.get();
                    String[] optNames = new String[optc];
                    for ( int i = 0; i < optc; i++ ) optNames[i] = readString(buf);
                    Map<String, Object> opts = new HashMap<>();
                    for ( int i = optc - 1 ; i >= 0; i-- ) opts.put(optNames[i], f.pop());
                    Object[] argv = new Object[argc];
                    for (int i = argc - 1; i >= 0; i--) argv[i] = f.pop();
                    if(f.hasFunction(name)) {
                        Function fn = f.getFunction(name);
                        f = new Frame(null, null, fn.definingFrame());
                        for(int i = 0; i < fn.parameters().size(); i++) {
                            FunctionParameter parameter = fn.parameters().get(i);
                            f.define("func." + parameter.name(), Variable.readOnly(argv[i]));
                        }
                        for(Map.Entry<String, Object> entry : opts.entrySet()) {
                            f.define("func." + entry.getKey(), Variable.readOnly(opts.containsKey(entry.getKey()) ? opts.get(entry.getKey()) : entry.getValue()));
                        }
                        stack.push(buf.position());
                        buf.position(fn.bodyStart());
                    }else {
                        f.push(host.call(name, Arrays.asList(argv), opts));
                    }
                }
                default -> throw new ScriptException("Unknown opcode: " + op);
            }
        }
        while (true);
    }



    private boolean truthy(Object pop) {
        if(pop instanceof Boolean) return (boolean) pop;
        if(pop instanceof Number) return ((Number) pop).doubleValue() != 0;
        return pop != StaticObjects.UNDEFINED && pop != StaticObjects.NULL;
    }

    private void store(Frame f, String name, Object object) {
        find(f, name).assign(object);

        f.push(object);
    }

    private void storeTMP(Frame f, String name, Object o) {
        if(!f.isTmp(name)) throw new ScriptException("Cannot store to non-tmp variable: " + name);
        f.get(name).set(o);
    }

    private void add(Frame f, Object object1, Object object2) {
        if(object1 instanceof Number && object2 instanceof Number) {
            if(object1 instanceof Integer && object2 instanceof Integer)
                f.push((Integer) object1 + (Integer) object2);
            else if(object1 instanceof Double || object2 instanceof Double)
                f.push(((Number) object1).doubleValue() + ((Number) object2).doubleValue());
            else throw new ScriptException("Unknown number type(s): " + typeName(object1) + " and " + typeName(object2));

        }
        else if((object2 instanceof List<?> list)) {
            @SuppressWarnings("unchecked") List<Object> list2 = (List<Object>)list;
            list2.add(object1);
            f.push(list2);
        }
        else if((object1 instanceof String) || (object2 instanceof String)) {
            f.push(object2.toString() + object1.toString());
        }
        else {
            throw new ScriptException("Cannot add " + typeName(object1) + " and " + typeName(object2));
        }
    }

    private void mul(Frame f, Object object1, Object object2) {
        if(object1 instanceof Number && object2 instanceof Number) {
            if(object1 instanceof Integer && object2 instanceof Integer)
                f.push((Integer) object1 * (Integer) object2);
            else if(object1 instanceof Double || object2 instanceof Double)
                f.push(((Number) object1).doubleValue() * ((Number) object2).doubleValue());
            else throw new ScriptException("Unknown number type(s): " + typeName(object1) + " and " + typeName(object2));

        }
        else if((object1 instanceof Integer) && (object2 instanceof String)) {
            f.push(object2.toString().repeat((int) object1));
        }
        else {
            throw new ScriptException("Cannot multiply " + typeName(object1) + " and " + typeName(object2));
        }
    }

    private void sub(Frame f, Object object1, Object object2) {
        if(object1 instanceof Number && object2 instanceof Number) {
            if(object1 instanceof Integer && object2 instanceof Integer)
                f.push((Integer) object2 - (Integer) object1);
            else if(object1 instanceof Double || object2 instanceof Double)
                f.push(((Number) object2).doubleValue() - ((Number) object1).doubleValue());
            else throw new ScriptException("Unknown number type(s): " + typeName(object1) + " and " + typeName(object2));
        }
    }
    private void div(Frame f, Object object1, Object object2) {
        if(object1 instanceof Number && object2 instanceof Number) {
            if(object1 instanceof Integer && object2 instanceof Integer)
                f.push((Integer) object2 / (Integer) object1);
            else if(object1 instanceof Double || object2 instanceof Double)
                f.push(((Number) object2).doubleValue() / ((Number) object1).doubleValue());
            else throw new ScriptException("Unknown number type(s): " + typeName(object1) + " and " + typeName(object2));
        }
    }

    private Object resolve(Frame f, String s) {
        //tmp, func, events, args, global, local
        return find(f, s).value();
    }

    private Map<String, Object> resolveRef(Frame f, String s) {
        Variable v = find(f, s);
        return Map.of("defined", v.isDefined(), "value", v.value());
    }

    private Variable find(Frame f, String s) {
        int dot = s.indexOf('.');
        if (dot > 0) {
            String scope = s.substring(0, dot), name = s.substring(dot + 1);
            return switch (scope) {
                case "func"  -> f.get("func." + name);
                case "local" -> f.get("local." + name);
                case "event" -> f.getEvent().getOrDefault(name, Variable.undefined());
                case "args"  -> f.getArgs().getOrDefault(name, Variable.undefined());
                case "glob"  -> globals.getOrDefault(name, Variable.undefined());
                default      -> Variable.undefined();
            };
        }
        if (f.isTmp(s))                         return f.get(s);
        Variable v = f.get("func." + s);
        if (v.isDefined())                      return v;
        if (f.getArgs().containsKey(s))         return f.getArgs().get(s);
        if (f.getEvent().containsKey(s))        return f.getEvent().get(s);
        v = f.get("local." + s);
        if (v.isDefined())                      return v;
        return globals.getOrDefault(s, Variable.undefined());
    }

    private String readString(ByteBuffer buf) {
        short len = buf.getShort();
        byte[] b = new byte[len];
        buf.get(b);
        return new String(b);
    }
    
    private Object readPushValue(ByteBuffer buf, Frame f) {
        PushType type = PushType.values()[buf.get()];
        return switch (type) {
            case INTEGER -> buf.getInt();
            case FLOAT -> buf.getDouble();
            case STRING -> readString(buf);
            case NULL -> StaticObjects.NULL;
            case UNDEFINED -> StaticObjects.UNDEFINED;
            case BOOLEAN -> buf.get() == 1;
            case ARRAY -> {
                int n = buf.getShort() & 0xFFFF;
                Object[] items = new Object[n];
                for (int i = n - 1; i >= 0; i--) items[i] = f.pop();
                yield new ArrayList<>(Arrays.asList(items));
            }
            case MAP -> {
                int n = buf.getShort() & 0xFFFF;
                Map<String, Object> map = new LinkedHashMap<>();
                // Compiler pusht pro Eintrag: Wert, dann Key -> Key wird zuerst gepoppt
                Object[][] entries = new Object[n][2];
                for (int i = n - 1; i >= 0; i--) {
                    entries[i][0] = f.pop();   // Key
                    entries[i][1] = f.pop();   // Wert
                }
                for (Object[] e : entries) map.put((String) e[0], e[1]);
                yield map;
            }
        };
    }
    private static String typeName(Object o) {
        if (o == null) return "null";
        if (o == StaticObjects.UNDEFINED) return "undefined";
        if (o instanceof Integer || o instanceof Double) return "number";
        if (o instanceof String) return "string";
        if (o instanceof Boolean) return "boolean";
        if (o instanceof List) return "list";
        if (o instanceof Map) return "map";
        return "object";
    }
}
