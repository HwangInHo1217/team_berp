// 5. com/team/berp/order/repository/OrderOrderLineItemRepository.java
package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.team.berp.domain.OrderLineItem;

public interface Order_OrderLineItemRepository extends JpaRepository<OrderLineItem, Long> {
}