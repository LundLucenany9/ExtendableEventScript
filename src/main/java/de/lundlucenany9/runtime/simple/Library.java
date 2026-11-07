package de.lundlucenany9.runtime.simple;

/**
 * Represents a named library that can be registered in the simple runtime host.
 * <p>
 * Libraries provide functions that may be exposed to scripts through annotated
 * methods.
 */
public interface Library {

    /**
     * Returns the unique name of this library.
     *
     * @return the library name
     */
    String getName();
}