// File: /Team_BERP/src/main/java/com/team/berp/domain/Employee.java
package com.team.berp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
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
    
}