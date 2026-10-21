package de.lundlucenany9.runtime.simple;

import de.lundlucenany9.runtime.Host;
import de.lundlucenany9.runtime.ImportException;
import de.lundlucenany9.runtime.ScriptHost;
import java.lang.reflect.Method;
import java.util.*;

public final class SimpleHost implements Host {

    private final List<EESMethod> methods;
    private final Map<String, Library> libraries;

    private SimpleHost(List<EESMethod> methods, Map<String, Library> libraries) {
        this.methods = methods;
        this.libraries = libraries;
    }

    public static SimpleHost getStandardHost() {
        return new Builder().addDefaultLibraries().build();
    }

    @Override
    public ScriptHost toScriptHost(List<String> imports) {
        if(imports.stream().anyMatch(e->!this.libraries.containsKey(e)))
            throw new ImportException("Library not found: " + imports.stream().filter(e->!this.libraries.containsKey(e)).findFirst().orElse(""));
        Map<String, Library> libraries = this.libraries.entrySet().stream().filter(e->imports.contains(e.getKey())).collect(HashMap::new, (m, e)->m.put(e.getKey(), e.getValue()), HashMap::putAll);
        List<EESMethod> methods = this.methods.stream().filter(e->imports.contains(e.libraryName())).toList();
        return new SimpleScriptHost(methods, libraries);
    }

    public static class Builder {
        private final Map<String, Library> libraries = new HashMap<>();

        public Builder() {}

        public Builder addDefaultLibraries() {
            registerLibrary(new StandardMethods());
            registerLibrary(new StandardIO());
            return this;
        }

        public Builder registerLibrary(Library library) {
            String name = library.getName();

            if (libraries.putIfAbsent(name, library) != null) {
                throw new LibraryRegistrationException(
                        "Library with name " + name + " already exists"
                );
            }
            return this;
        }

        public SimpleHost build() {
            List<EESMethod> methods = new ArrayList<>();
            for(Library library : libraries.values()) {
                String libraryName = library.getName();
                for(Method method : library.getClass().getDeclaredMethods()) {
                    if(!method.isAnnotationPresent(EESFunction.class)) continue;
                    String name = method.getAnnotation(EESFunction.class).name();
                    if(name.isBlank())
                        name = method.getName();
                    methods.add(new EESMethod(libraryName, name, method));
                }
            }
            return new SimpleHost(methods, libraries);
        }
    }
}
