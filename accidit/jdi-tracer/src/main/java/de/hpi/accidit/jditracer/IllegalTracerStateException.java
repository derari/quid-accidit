package de.hpi.accidit.jditracer;

public class IllegalTracerStateException extends IllegalStateException {

    public IllegalTracerStateException() {
    }

    public IllegalTracerStateException(String s) {
        super(s);
    }

    public IllegalTracerStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalTracerStateException(Throwable cause) {
        super(cause);
    }
    
}
