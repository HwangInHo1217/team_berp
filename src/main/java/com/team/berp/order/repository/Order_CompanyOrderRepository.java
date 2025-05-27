// 4. com/team/berp/order/repository/OrderCompanyOrderRepository.java
package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.team.berp.domain.CompanyOrder;

/**
 * Repository for CompanyOrder entities (주문 헤더 관리).
 */
@Repository
public interface Order_CompanyOrderRepository extends JpaRepository<CompanyOrder, Long> {
    // 주문번호로 조회
    // Optional<CompanyOrder> findByOrderId(Long orderId);
}