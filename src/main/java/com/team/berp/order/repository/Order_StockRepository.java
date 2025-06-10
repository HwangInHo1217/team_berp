// File: src/main/java/com/team/berp/order/repository/Order_StockRepository.java
package com.team.berp.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Stock;

public interface Order_StockRepository extends JpaRepository<Stock, Long> {
    // itemId와 수량(qty) 기준으로 재고가 qty 초과인 Stock 목록 조회
    List<Stock> findByItemIdAndQuantityGreaterThan(Long itemId, Integer qty);
}
