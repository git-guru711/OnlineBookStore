package com.example.bookstore.exception;

/** A concurrent request changed the same data (409, safe to retry). */
public class ConcurrentRequestException extends RuntimeException {

    public ConcurrentRequestException() {
        super("The data was changed by another request. Please reload and try again.");
    }
}
