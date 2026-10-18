package de.lundlucenany9.runtime.simple;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface EESOptional {
    String name();
    String stringValue() default "";
    int intValue() default 0;
    boolean booleanValue() default false;
    double doubleValue() default 0.0;

    Type type();

    enum Type {
        STRING,
        INT,
        BOOLEAN,
        DOUBLE,
    }
}

