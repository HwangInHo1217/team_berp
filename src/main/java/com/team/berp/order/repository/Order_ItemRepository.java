package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Item;

public interface Order_ItemRepository extends JpaRepository<Item, Long> {

}
