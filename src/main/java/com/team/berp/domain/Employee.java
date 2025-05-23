// File: /Team_BERP/src/main/java/com/team/berp/domain/Employee.java
package com.team.berp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @Column(nullable = false, length = 50)
    private String empName;

    @Column(nullable = false, length = 100, unique = true)
    private String empEmail;

    @Column(length = 20)
    private String empTel;

    @Column(length = 20)
    private String empHp;

    // getters and setters
    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getEmpEmail() {
        return empEmail;
    }

    public void setEmpEmail(String empEmail) {
        this.empEmail = empEmail;
    }

    public String getEmpTel() {
        return empTel;
    }

    public void setEmpTel(String empTel) {
        this.empTel = empTel;
    }

    public String getEmpHp() {
        return empHp;
    }

    public void setEmpHp(String empHp) {
        this.empHp = empHp;
    }
}