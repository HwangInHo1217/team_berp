package com.team.berp.shipment.repository;

import com.team.berp.shipment.dto.OrderSummaryDto;
import com.team.berp.domain.CompanyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * CompanyOrder 엔티티 전용 Repository
 * 아직 출고되지 않은 주문 목록 조회용
 */
@Repository
public interface Shipment_CompanyOrderRepository extends JpaRepository<CompanyOrder, Long> {

    @Query("""
      SELECT new com.team.berp.shipment.dto.OrderSummaryDto(
        o.orderId, o.orderDate, c.companyName, o.orderQty, o.amount,
        e.empName, c.companyEmpName
      )
      FROM CompanyOrder o
       JOIN o.company c
       JOIN c.employee e
      WHERE o.orderId NOT IN (
        SELECT l.orderLineItem.companyOrder.orderId
          FROM InventoryLog l
      )
    """)
    List<OrderSummaryDto> findPendingOrders();

    @Query("""
      SELECT o FROM CompanyOrder o
       JOIN FETCH o.company c
       JOIN FETCH c.employee e
       JOIN FETCH o.lineItems li
       JOIN FETCH li.item it
       JOIN FETCH li.warehouse w
      WHERE o.orderId = :orderId
    """)
    CompanyOrder findWithDetails(Long orderId);
}