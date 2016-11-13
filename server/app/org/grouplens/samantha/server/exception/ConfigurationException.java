package org.grouplens.samantha.server.exception;

public class ConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
