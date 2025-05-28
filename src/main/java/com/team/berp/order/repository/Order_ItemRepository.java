package com.example.order.repository;

import com.example.order.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    /**
     * 품목명으로 품목 엔티티 조회
     */
    Item findByItemName(String itemName);
}