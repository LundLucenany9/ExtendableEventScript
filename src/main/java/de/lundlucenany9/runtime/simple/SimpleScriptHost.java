package de.lundlucenany9.runtime.simple;

import de.lundlucenany9.runtime.ScriptHost;
import de.lundlucenany9.runtime.StaticObjects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Simple {@link ScriptHost} implementation used to invoke registered library
 * functions from scripts.
 * <p>
 * The host resolves exported methods, maps script arguments to Java parameters
 * and dispatches calls to the corresponding library instances.
 */

public final class SimpleScriptHost implements ScriptHost {
    private final List<EESMethod> methods;
    private final Map<String, Library> libraries;
    /**
     * Creates a script host using the given exported methods and libraries.
     *
     * @param methods   the exported methods that scripts may call
     * @param libraries the registered libraries that own the methods
     */
    SimpleScriptHost(List<EESMethod> methods, Map<String, Library> libraries) {
        this.methods = methods;
        this.libraries = libraries;
    }

    /**
     * Calls an exported script function by name.
     *
     * @param name the script-visible function name
     * @param args the positional arguments passed by the script
     * @param opts the optional named arguments passed by the script
     * @return the function result
     */
    @Override
    public Object call(String name, List<Object> args, Map<String, Object> opts) {
        if(name.contains(".")) {
            int sep = name.lastIndexOf(".");
            String library = name.substring(0, sep);
            name = name.substring(sep + 1);
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

    /**
     * Invokes a reflected library method with mapped script arguments.
     *
     * @param library the name of the library that owns the method
     * @param method  the reflected Java method to invoke
     * @param args    the positional script arguments
     * @param opts    the optional named script arguments
     * @return the invocation result
     */
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
                    cause != null ? cause : e
            );
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new MethodResolutionError(e);
        }
    }
}
