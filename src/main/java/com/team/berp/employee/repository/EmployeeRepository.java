package com.team.berp.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

}
