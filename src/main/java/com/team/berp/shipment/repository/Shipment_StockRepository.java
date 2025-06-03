package com.team.berp.shipment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Stock;

public interface Shipment_StockRepository extends JpaRepository<Stock, Long>{
	   /**
     * 특정 품목(itemId) + 창고(warehouseId) 해당하는 Stock 엔티티 조회
     */
	List<Stock> findByItemIdAndWarehouseId(Long itemId, Long warehouseId);
}
