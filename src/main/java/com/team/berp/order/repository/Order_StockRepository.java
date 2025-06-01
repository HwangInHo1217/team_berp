package com.team.berp.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Stock;

public interface Order_StockRepository extends JpaRepository<Stock, Long> {
	// itemId와 수량 조건으로 창고 재고 조회
	List<Stock> findByItemIdAndQuantityGreaterThan(Long itemId, int minQty);

}
