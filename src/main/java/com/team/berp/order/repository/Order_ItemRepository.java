// File: src/main/java/com/team/berp/order/repository/Order_ItemRepository.java
package com.team.berp.order.repository;

import com.team.berp.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

/** 품목(Item) 조회용 */
public interface Order_ItemRepository extends JpaRepository<Item, Long> {
    Item findByName(String name);
}