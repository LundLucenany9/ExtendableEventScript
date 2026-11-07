package de.lundlucenany9.runtime.simple;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a function parameter as optional for script calls.
 * <p>
 * The annotation name can be used to identify the optional argument when
 * resolving script-provided parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface EESOptional {

    /**
     * Returns the script-visible name of the optional parameter.
     *
     * @return the optional parameter name
     */
    String name();
}