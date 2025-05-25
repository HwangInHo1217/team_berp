// File: /Team_BERP/src/main/java/com/team/berp/domain/Employee.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee")
@Getter
@Setter
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer employee_id;

    @Column(nullable = false, length = 50)
    private String emp_name;

    @Column(nullable = false, length = 100, unique = true)
    private String emp_email;

    @Column(length = 20)
    private String emp_tel;

    @Column(length = 20)
    private String emp_hp;
}