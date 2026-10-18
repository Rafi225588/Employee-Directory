package com.example.employee_directory.service;

import com.example.employee_directory.dto.EmployeeRequest;
import com.example.employee_directory.dto.EmployeeResponse;
import com.example.employee_directory.entity.Employee;
import com.example.employee_directory.exception.DuplicateEmailException;
import com.example.employee_directory.exception.EmployeeFileNotFoundException;
import com.example.employee_directory.exception.EmployeeNotFoundException;
import com.example.employee_directory.exception.InvalidFileException;
import com.example.employee_directory.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @TempDir
    Path tempDir;

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

    private EmployeeService createFileService() {
        return new EmployeeService(employeeRepository, tempDir.toString());
    }

    private Path getStoredFile(String fileUrl) {
        return tempDir.resolve(fileUrl.substring(fileUrl.lastIndexOf("/") + 1));
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

    @Test
    void uploadFileSavesFileInFolderAndStoresUrl() {
        EmployeeService fileService = createFileService();
        Employee employee = new Employee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "pdf content".getBytes());

        EmployeeResponse response = fileService.uploadFile(1L, file);

        assertTrue(response.getFileUrl().startsWith("/uploads/"));
        assertTrue(response.getFileUrl().endsWith(".pdf"));
        assertTrue(Files.exists(getStoredFile(response.getFileUrl())));
        verify(employeeRepository).save(employee);
    }

    @Test
    void uploadFileReplacesOldFile() throws IOException {
        EmployeeService fileService = createFileService();
        Employee employee = new Employee();
        employee.setFileUrl("/uploads/old-file.pdf");
        Path oldFile = Files.writeString(tempDir.resolve("old-file.pdf"), "old content");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "image content".getBytes());

        EmployeeResponse response = fileService.uploadFile(1L, file);

        assertFalse(Files.exists(oldFile));
        assertTrue(response.getFileUrl().endsWith(".png"));
        assertTrue(Files.exists(getStoredFile(response.getFileUrl())));
    }

    @Test
    void uploadFileThrowsWhenFileTypeIsNotAllowed() {
        EmployeeService fileService = createFileService();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(new Employee()));
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        assertThrows(InvalidFileException.class, () -> fileService.uploadFile(1L, file));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void uploadFileThrowsWhenFileIsEmpty() {
        EmployeeService fileService = createFileService();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(new Employee()));
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(InvalidFileException.class, () -> fileService.uploadFile(1L, file));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void deleteFileRemovesFileAndUrl() throws IOException {
        EmployeeService fileService = createFileService();
        Employee employee = new Employee();
        employee.setFileUrl("/uploads/cv-file.pdf");
        Path storedFile = Files.writeString(tempDir.resolve("cv-file.pdf"), "content");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        fileService.deleteFile(1L);

        assertNull(employee.getFileUrl());
        assertFalse(Files.exists(storedFile));
        verify(employeeRepository).save(employee);
    }

    @Test
    void deleteFileThrowsWhenEmployeeHasNoFile() {
        EmployeeService fileService = createFileService();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(new Employee()));

        assertThrows(EmployeeFileNotFoundException.class, () -> fileService.deleteFile(1L));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void deleteEmployeeAlsoRemovesItsFile() throws IOException {
        EmployeeService fileService = createFileService();
        Employee employee = new Employee();
        employee.setFileUrl("/uploads/cv-file.pdf");
        Path storedFile = Files.writeString(tempDir.resolve("cv-file.pdf"), "content");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        fileService.deleteEmployee(1L);

        assertFalse(Files.exists(storedFile));
        verify(employeeRepository).delete(employee);
    }
}