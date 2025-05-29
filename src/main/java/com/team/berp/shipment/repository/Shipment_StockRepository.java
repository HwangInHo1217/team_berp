package com.team.berp.shipment.repository;

import com.team.berp.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Stock 엔티티 전용 Repository
 * 재고 수량 조회·수정용
 */
@Repository
public interface Shipment_StockRepository extends JpaRepository<Stock, Long> {
}