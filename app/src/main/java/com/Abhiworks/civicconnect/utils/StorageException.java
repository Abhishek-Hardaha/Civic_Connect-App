package com.Abhiworks.civicconnect.utils;

/** Thrown when a Supabase Storage upload or URL fetch fails. */
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
