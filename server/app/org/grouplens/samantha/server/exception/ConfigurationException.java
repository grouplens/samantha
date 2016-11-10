package org.grouplens.samantha.server.exception;

public class ConfigurationException extends RuntimeException {
    //TODO: adding serialVersionUID, not sure why
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
