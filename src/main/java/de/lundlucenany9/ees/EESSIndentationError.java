package de.lundlucenany9.ees;

public class EESSIndentationError extends RuntimeException {
    public EESSIndentationError(String message) {
        super(message);
    }
    public EESSIndentationError(Throwable e) {
      super(e);
    }
}
