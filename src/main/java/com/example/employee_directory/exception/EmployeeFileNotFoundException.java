package com.example.employee_directory.exception;

public class EmployeeFileNotFoundException extends RuntimeException {

    public EmployeeFileNotFoundException(Long id) {
        super("No file found for employee id: " + id);
    }
}