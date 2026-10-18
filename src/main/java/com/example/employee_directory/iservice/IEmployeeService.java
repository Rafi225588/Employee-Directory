package com.example.employee_directory.iservice;

import com.example.employee_directory.dto.EmployeeRequest;
import com.example.employee_directory.dto.EmployeeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IEmployeeService {

    EmployeeResponse createEmployee(EmployeeRequest request);

    List<EmployeeResponse> getAllEmployees();

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);
    EmployeeResponse uploadFile(Long id, MultipartFile file);

    void deleteFile(Long id);
}
