package com.loft.loftintegration.connector;

/** Thrown when a connector cannot establish or maintain a session with the PMS. */
public class PmsConnectionException extends Exception {

    public PmsConnectionException(String message) {
        super(message);
    }

    public PmsConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
