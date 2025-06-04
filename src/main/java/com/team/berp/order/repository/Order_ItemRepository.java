package com.team.berp.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Item;
import com.team.berp.order.apicontroller.OrderApiController.ItemDto;

public interface Order_ItemRepository extends JpaRepository<Item, Long> {
	
	
}
