// File: src/main/java/com/team/berp/order/repository/Order_OrderLineItemRepository.java
package com.team.berp.order.repository;

import com.team.berp.domain.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

/** 주문 라인 아이템 CRUD */
public interface Order_OrderLineItemRepository extends JpaRepository<OrderLineItem, Long> { }