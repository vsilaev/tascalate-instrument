package net.tascalate.instrument.emitter.spi;

public class ClassEmitterException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public ClassEmitterException(String message) {
        super(message);
    }
    
    public ClassEmitterException(Throwable cause) {
        super(cause);
    }
    
    public ClassEmitterException(String message, Throwable cause) {
        super(message, cause);
    }

    
}
