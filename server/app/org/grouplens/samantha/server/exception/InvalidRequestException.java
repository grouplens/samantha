package org.grouplens.samantha.server.exception;

public class InvalidRequestException extends RuntimeException {
    //TODO: adding serialVersionUID
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
