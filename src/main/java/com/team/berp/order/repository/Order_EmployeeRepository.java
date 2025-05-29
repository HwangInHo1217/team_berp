// File: src/main/java/com/team/berp/order/repository/Order_EmployeeRepository.java
package com.team.berp.order.repository;

import com.team.berp.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

/** 우리회사 직원(Employee) 조회 용  */
public interface Order_EmployeeRepository extends JpaRepository<Employee, Long> { }