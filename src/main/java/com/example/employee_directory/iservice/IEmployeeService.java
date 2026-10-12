package com.example.employee_directory.iservice;

import com.example.employee_directory.dto.EmployeeRequest;
import com.example.employee_directory.dto.EmployeeResponse;

import java.util.List;

public interface IEmployeeService {

    EmployeeResponse createEmployee(EmployeeRequest request);

    List<EmployeeResponse> getAllEmployees();

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);
}
