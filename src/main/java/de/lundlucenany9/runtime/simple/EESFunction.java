package de.lundlucenany9.runtime.simple;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java method as a function that can be exposed to scripts.
 * <p>
 * If no explicit name is provided, the runtime may use the Java method name
 * as the exported function name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EESFunction {

    /**
     * Returns the script-visible function name.
     *
     * @return the exported function name, or an empty string to use the Java method name
     */
    String name() default "";
}