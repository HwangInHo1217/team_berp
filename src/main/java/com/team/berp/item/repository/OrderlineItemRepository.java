package com.team.berp.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.OrderLineItem;

public interface OrderlineItemRepository extends JpaRepository<OrderLineItem, Long> {

}
