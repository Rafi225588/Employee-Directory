package com.example.employee_directory.service;

import com.example.employee_directory.dto.EmployeeRequest;
import com.example.employee_directory.dto.EmployeeResponse;
import com.example.employee_directory.entity.Employee;
import com.example.employee_directory.exception.DuplicateEmailException;
import com.example.employee_directory.exception.EmployeeNotFoundException;
import com.example.employee_directory.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private EmployeeRequest createRequest() {
        EmployeeRequest request = new EmployeeRequest();
        request.setFullName("Karim Hasan");
        request.setEmail("Karim@Example.com");
        request.setPhone("01712345678");
        request.setDepartment("Engineering");
        request.setDesignation("Intern");
        request.setJoiningDate(LocalDate.of(2026, 10, 1));
        return request;
    }

    @Test
    void createEmployeeSavesAndReturnsResponse() {
        when(employeeRepository.existsByEmail("karim@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = employeeService.createEmployee(createRequest());

        assertEquals("Karim Hasan", response.getFullName());
        assertEquals("karim@example.com", response.getEmail());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void createEmployeeThrowsWhenEmailAlreadyExists() {
        when(employeeRepository.existsByEmail("karim@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> employeeService.createEmployee(createRequest()));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void getAllEmployeesReturnsAllRecords() {
        when(employeeRepository.findAll()).thenReturn(List.of(new Employee(), new Employee()));

        List<EmployeeResponse> responses = employeeService.getAllEmployees();

        assertEquals(2, responses.size());
    }

    @Test
    void getEmployeeByIdReturnsEmployee() {
        Employee employee = new Employee();
        employee.setFullName("Karim Hasan");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertEquals("Karim Hasan", response.getFullName());
    }

    @Test
    void getEmployeeByIdThrowsWhenIdNotFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(999L));
    }

    @Test
    void updateEmployeeThrowsWhenIdNotFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(999L, createRequest()));
    }

    @Test
    void updateEmployeeThrowsWhenEmailBelongsToAnotherEmployee() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(new Employee()));
        when(employeeRepository.existsByEmailAndIdNot("karim@example.com", 1L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> employeeService.updateEmployee(1L, createRequest()));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void deleteEmployeeRemovesExistingEmployee() {
        Employee employee = new Employee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void deleteEmployeeThrowsWhenIdNotFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(999L));
        verify(employeeRepository, never()).delete(any(Employee.class));
    }
}
