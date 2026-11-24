package com.example.bookstore.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Email %s is already registered".formatted(email));
    }
}
