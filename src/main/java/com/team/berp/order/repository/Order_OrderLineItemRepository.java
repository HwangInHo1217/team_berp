// OrderLineItemRepository.java
package com.team.berp.order.repository;

import com.team.berp.domain.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 주문 품목(OrderLineItem) 엔티티 CRUD용 레포지토리
 */
public interface Order_OrderLineItemRepository extends JpaRepository<OrderLineItem, Long> {
}