package com.Abhiworks.civicconnect.utils;

/** Thrown when a network request fails (no connection, timeout, etc.) */
public class NetworkException extends RuntimeException {
    public NetworkException(String message) {
        super(message);
    }
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
