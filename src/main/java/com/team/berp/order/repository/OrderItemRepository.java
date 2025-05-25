package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Item;

//공동 도메인의 엔티티를 참조함
public interface OrderItemRepository extends JpaRepository<Item, Integer> {

}
