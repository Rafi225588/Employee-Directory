package com.example.employee_directory.service;

import com.example.employee_directory.dto.EmployeeRequest;
import com.example.employee_directory.dto.EmployeeResponse;
import com.example.employee_directory.entity.Employee;
import com.example.employee_directory.exception.DuplicateEmailException;
import com.example.employee_directory.exception.EmployeeNotFoundException;
import com.example.employee_directory.iservice.IEmployeeService;
import com.example.employee_directory.repository.EmployeeRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService implements IEmployeeService {
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
    private Employee findEmployeeById(Long id){
        Optional<Employee> optionalEmployee= employeeRepository.findById(id);
        if (optionalEmployee.isEmpty()){
            throw new EmployeeNotFoundException(id);
        }
        return optionalEmployee.get();
    }
    private String normalizeEmail(String email){
        return email.trim().toLowerCase();
    }
    private void copyRequestToEntity(EmployeeRequest request, Employee employee) {
        BeanUtils.copyProperties(request, employee);
        employee.setEmail(normalizeEmail(request.getEmail()));
    }

    private EmployeeResponse convertToResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        BeanUtils.copyProperties(employee, response);
        return response;
    }
}
