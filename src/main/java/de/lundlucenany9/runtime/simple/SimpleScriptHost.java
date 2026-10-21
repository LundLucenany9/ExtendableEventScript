package de.lundlucenany9.runtime.simple;

import de.lundlucenany9.runtime.ScriptHost;
import de.lundlucenany9.runtime.StaticObjects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public final class SimpleScriptHost implements ScriptHost {
    private final List<EESMethod> methods;
    private final Map<String, Library> libraries;
    SimpleScriptHost(List<EESMethod> methods, Map<String, Library> libraries) {
        this.methods = methods;
        this.libraries = libraries;
    }
    @Override
    public Object call(String name, List<Object> args, Map<String, Object> opts) {
        if(name.contains(".")) {
            String[] parts = name.split("\\.", -1);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new MethodResolutionError("Invalid method name: " + name);
            }
            String library = parts[0];
            name = parts[1];
            String finalName = name;
            Optional<EESMethod> method = methods.stream().filter(e-> e.libraryName().equals(library) && e.methodName().equals(finalName)).findFirst();
            Method m = method.map(EESMethod::method).orElse(null);
            if(m == null) {
                throw new MethodResolutionError("Method not found: " + library + "." + name);
            }
            return run(library,m, args, opts);
        }
        String finalName = name;
        Optional<EESMethod> method = methods.stream().filter(e->e.methodName().equals(finalName)).findFirst();
        Method m = method.map(EESMethod::method).orElse(null);
        if(m == null) {
            throw new MethodResolutionError("Method not found: " + name);
        }
        return run(method.get().libraryName(), m, args, opts);
    }

    private Object run(String library, Method method, List<Object> args, Map<String, Object> opts) {
        method.setAccessible(true);
        Parameter[] p = method.getParameters();
        long parameters = Arrays.stream(p).filter(e-> !e.isAnnotationPresent(EESOptional.class)).count();
        if(parameters != args.size()) {
            throw new MethodResolutionError("Wrong number of arguments");
        }
        List<Object> params = new ArrayList<>();
        int i = 0;
        for(Parameter parameter : p) {
            if(parameter.isAnnotationPresent(EESOptional.class)) {
                Object value;
                EESOptional optional = parameter.getAnnotation(EESOptional.class);
                String name = optional.name();
                if(parameter.getType() == Optional.class) {
                    if (opts.containsKey(name)) {
                        value = Optional.of(opts.get(name));
                    } else {
                        value = Optional.empty();
                    }
                } else {
                    value = opts.getOrDefault(name, null);
                }
                params.add(value);
                continue;
            }
            params.add(args.get(i++));
        }
        try {

            Object result = method.invoke(libraries.get(library), params.toArray());
            if(method.getReturnType() == Void.TYPE) return StaticObjects.UNDEFINED;
            if(result == null) return StaticObjects.NULL;
            return result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new MethodResolutionError(
                    cause != null ? cause.getMessage() : e.getMessage()
            );
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new MethodResolutionError(e.getMessage());
        }
    }
}
