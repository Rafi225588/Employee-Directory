package com.example.employee_directory.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="employees")
public class Employee {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name="id")
    private Long id;
    @Column (name="full_name",nullable = false, length=100)
    private String fullName;
    @Column (name="email",nullable = false,unique = true,length=255)
    private String email;
    @Column (name="phone",nullable=false,length=11)
    private String phone;
    @Column (name="department",nullable=false,length=255)
    private String department;
    @Column (name="designation",length = 255)
    private String designation;
    @Column (name="joining_date",nullable=false)
    private LocalDate joiningDate;
    @Column (name="created_at",nullable= false,updatable = false)
    private LocalDateTime createdAt;
    @Column (name="updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "file_url")
    private String fileUrl;
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
