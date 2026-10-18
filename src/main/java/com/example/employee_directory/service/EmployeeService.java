package com.example.employee_directory.service;

import com.example.employee_directory.dto.EmployeeRequest;
import com.example.employee_directory.dto.EmployeeResponse;
import com.example.employee_directory.entity.Employee;
import com.example.employee_directory.exception.DuplicateEmailException;
import com.example.employee_directory.exception.EmployeeFileNotFoundException;
import com.example.employee_directory.exception.EmployeeNotFoundException;
import com.example.employee_directory.exception.InvalidFileException;
import com.example.employee_directory.iservice.IEmployeeService;
import com.example.employee_directory.repository.EmployeeRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService implements IEmployeeService {
    private static final List<String> ALLOWED_FILE_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (employeeRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);

        }
        Employee employee = new Employee();
        copyRequestToEntity(request, employee);
        Employee savedEmployee = employeeRepository.save(employee);
        return convertToResponse(savedEmployee);
    }
    @Override
    public List<EmployeeResponse> getAllEmployees(){
        List<Employee> employees=employeeRepository.findAll();
        List<EmployeeResponse>responses= new ArrayList<>();
        for(Employee employee:employees){
            responses.add(convertToResponse(employee));

        }
        return responses;
    }
    @Override
    public EmployeeResponse getEmployeeById(Long id){
        Employee employee= findEmployeeById(id);
        return convertToResponse(employee);
    }
    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request){
        Employee employee= findEmployeeById(id);
        String email= normalizeEmail(request.getEmail());
        if (employeeRepository.existsByEmailAndIdNot(email, id)){
            throw new DuplicateEmailException(email);
        }
        copyRequestToEntity(request, employee);
        Employee updatedEmployee= employeeRepository.save(employee);
        return convertToResponse(updatedEmployee);

    }
    @Override
    public void deleteEmployee(Long id){
        Employee employee= findEmployeeById(id);
        employeeRepository.delete(employee);
    }
    @Override
    public EmployeeResponse uploadFile(Long id, MultipartFile file) {
        Employee employee = findEmployeeById(id);
        validateFile(file);
        try {
            employee.setFileData(file.getBytes());
        } catch (IOException e) {
            throw new InvalidFileException("File could not be read");
        }
        employee.setFileName(file.getOriginalFilename());
        employee.setFileType(file.getContentType());
        Employee savedEmployee = employeeRepository.save(employee);
        return convertToResponse(savedEmployee);
    }
    @Override
    public Employee getFile(Long id) {
        return findEmployeeWithFile(id);
    }

    @Override
    public void deleteFile(Long id) {
        Employee employee = findEmployeeWithFile(id);
        employee.setFileData(null);
        employee.setFileName(null);
        employee.setFileType(null);
        employeeRepository.save(employee);
    }
    private Employee findEmployeeById(Long id){
        Optional<Employee> optionalEmployee= employeeRepository.findById(id);
        if (optionalEmployee.isEmpty()){
            throw new EmployeeNotFoundException(id);
        }
        return optionalEmployee.get();
    }
    private Employee findEmployeeWithFile(Long id) {
        Employee employee = findEmployeeById(id);
        if (employee.getFileData() == null) {
            throw new EmployeeFileNotFoundException(id);
        }
        return employee;
    }
    private String normalizeEmail(String email){
        return email.trim().toLowerCase();
    }
    private void copyRequestToEntity(EmployeeRequest request, Employee employee) {
        BeanUtils.copyProperties(request, employee);
        employee.setEmail(normalizeEmail(request.getEmail()));
    }
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please select a file to upload");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
            throw new InvalidFileException("Only PDF and image files are allowed");
        }
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.length() > 255) {
            throw new InvalidFileException("File name must not exceed 255 characters");
        }
    }

    private EmployeeResponse convertToResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        BeanUtils.copyProperties(employee, response);
        response.setHasFile(employee.getFileData() != null);
        return response;
    }
}
