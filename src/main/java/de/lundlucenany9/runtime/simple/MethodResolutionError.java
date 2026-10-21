package de.lundlucenany9.runtime.simple;

public class MethodResolutionError extends RuntimeException {
    public MethodResolutionError(String message) {
        super(message);
    }
    public MethodResolutionError(Throwable e) {
      super(e);
    }
}
