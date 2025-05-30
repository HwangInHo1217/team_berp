package com.team.berp.shipment.repository;

import com.team.berp.domain.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * InventoryLog 엔티티 전용 Repository
 * 출고 내역 조회용
 */
@Repository
public interface Shipment_InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    /**
     * 모든 출고 내역(InventoryLog) + 연관 주문/고객사/담당자 한 번에 조회
     */
    @Query("""
      SELECT l FROM InventoryLog l
       JOIN FETCH l.orderLineItem oli
       JOIN FETCH oli.companyOrder o
       JOIN FETCH o.company c
       JOIN FETCH c.employee e
      """)
    List<InventoryLog> findAllShipments();

    /**
     * 단일 출고 내역 상세 조회 (모달용)
     */
    @Query("""
      SELECT l FROM InventoryLog l
       JOIN FETCH l.orderLineItem oli
       JOIN FETCH oli.companyOrder o
       JOIN FETCH o.company c
       JOIN FETCH c.employee e
      WHERE l.id = :id
    """)
    InventoryLog findWithDetails(Long id);
}