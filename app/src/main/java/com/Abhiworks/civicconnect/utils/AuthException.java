package com.Abhiworks.civicconnect.utils;

/** Thrown on 401 responses that survive a token-refresh retry. */
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
