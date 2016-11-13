package org.grouplens.samantha.server.exception;

public class InvalidRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidRequestException() {
        super();
    }

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(Throwable cause) {
        super(cause);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
