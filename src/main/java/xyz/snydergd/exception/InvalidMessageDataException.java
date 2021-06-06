package xyz.snydergd.exception;

public class InvalidMessageDataException extends Exception {

    public InvalidMessageDataException() {
    }

    public InvalidMessageDataException(String message) {
        super(message);
    }

    public InvalidMessageDataException(Throwable cause) {
        super(cause);
    }

    public InvalidMessageDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidMessageDataException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
