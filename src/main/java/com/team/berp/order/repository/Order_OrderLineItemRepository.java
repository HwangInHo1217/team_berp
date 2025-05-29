package com.team.berp.order.repository;

import com.team.berp.domain.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Order_OrderLineItemRepository
    extends JpaRepository<OrderLineItem, Long> { }
