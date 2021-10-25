package won.shacl2java.runtime.model;

public class NotASingletonPropertyException extends RuntimeException {
    public NotASingletonPropertyException() {
    }

    public NotASingletonPropertyException(String message) {
        super(message);
    }

    public NotASingletonPropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotASingletonPropertyException(Throwable cause) {
        super(cause);
    }

    public NotASingletonPropertyException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
