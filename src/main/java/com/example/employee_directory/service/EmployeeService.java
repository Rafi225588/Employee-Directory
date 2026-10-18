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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class EmployeeService implements IEmployeeService {

    private static final Map<String, String> ALLOWED_FILE_TYPES = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final EmployeeRepository employeeRepository;
    private final String uploadDir;

    public EmployeeService(EmployeeRepository employeeRepository, @Value("${app.upload-dir}") String uploadDir) {
        this.employeeRepository = employeeRepository;
        this.uploadDir = uploadDir;
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
    public List<EmployeeResponse> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        List<EmployeeResponse> responses = new ArrayList<>();
        for (Employee employee : employees) {
            responses.add(convertToResponse(employee));
        }
        return responses;
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = findEmployeeById(id);
        return convertToResponse(employee);
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = findEmployeeById(id);
        String email = normalizeEmail(request.getEmail());
        if (employeeRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }
        copyRequestToEntity(request, employee);
        Employee updatedEmployee = employeeRepository.save(employee);
        return convertToResponse(updatedEmployee);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = findEmployeeById(id);
        String fileUrl = employee.getFileUrl();
        employeeRepository.delete(employee);
        deleteFileFromFolder(fileUrl);
    }

    @Override
    public EmployeeResponse uploadFile(Long id, MultipartFile file) {
        Employee employee = findEmployeeById(id);
        validateFile(file);
        String oldFileUrl = employee.getFileUrl();
        String newFileUrl = saveFileToFolder(file);
        employee.setFileUrl(newFileUrl);
        Employee savedEmployee = employeeRepository.save(employee);
        deleteFileFromFolder(oldFileUrl);
        return convertToResponse(savedEmployee);
    }

    @Override
    public void deleteFile(Long id) {
        Employee employee = findEmployeeById(id);
        String fileUrl = employee.getFileUrl();
        if (fileUrl == null) {
            throw new EmployeeFileNotFoundException(id);
        }
        employee.setFileUrl(null);
        employeeRepository.save(employee);
        deleteFileFromFolder(fileUrl);
    }

    private Employee findEmployeeById(Long id) {
        Optional<Employee> optionalEmployee = employeeRepository.findById(id);
        if (optionalEmployee.isEmpty()) {
            throw new EmployeeNotFoundException(id);
        }
        return optionalEmployee.get();
    }

    private String normalizeEmail(String email) {
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
        if (contentType == null || !ALLOWED_FILE_TYPES.containsKey(contentType)) {
            throw new InvalidFileException("Only PDF and image files are allowed");
        }
    }

    private Path getUploadFolder() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String saveFileToFolder(MultipartFile file) {
        String extension = ALLOWED_FILE_TYPES.get(file.getContentType());
        String storedName = UUID.randomUUID() + extension;
        Path folder = getUploadFolder();
        try {
            Files.createDirectories(folder);
            file.transferTo(folder.resolve(storedName));
        } catch (IOException e) {
            throw new InvalidFileException("File could not be saved");
        }
        return "/uploads/" + storedName;
    }

    private void deleteFileFromFolder(String fileUrl) {
        if (fileUrl == null) {
            return;
        }
        String storedName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        try {
            Files.deleteIfExists(getUploadFolder().resolve(storedName));
        } catch (IOException e) {
            throw new InvalidFileException("File could not be deleted");
        }
    }

    private EmployeeResponse convertToResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        BeanUtils.copyProperties(employee, response);
        return response;
    }
}