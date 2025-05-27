// 2. com/team/berp/order/repository/OrderEmployeeRepository.java
package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.team.berp.domain.Employee;

/**
 * Repository for Employee entities (주문 담당자 관리).
 */
@Repository
public interface Order_EmployeeRepository extends JpaRepository<Employee, Long> {
    // 예: 담당자 이름으로 검색
    // List<Employee> findByEmpNameContaining(String name);
}