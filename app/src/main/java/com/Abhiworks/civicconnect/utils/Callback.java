package com.Abhiworks.civicconnect.utils;

/**
 * Generic async callback for all repository calls.
 * onSuccess is always dispatched on the main thread by callers.
 * onError is always dispatched on the main thread by callers.
 */
public interface Callback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
