package com.team.berp.order.repository;

import com.team.berp.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Order_EmployeeRepository
    extends JpaRepository<Employee, Long> { }
