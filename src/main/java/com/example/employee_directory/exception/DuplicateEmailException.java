package com.example.employee_directory.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Employee already exists with email: " + email);
    }
}
