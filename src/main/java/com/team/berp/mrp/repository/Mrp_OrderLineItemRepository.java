// File: /Team_BERP/src/main/java/com/team/berp/mrp/repository/OrderLineItemRepository.java
package com.team.berp.mrp.repository;

import com.team.berp.domain.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Mrp_OrderLineItemRepository extends JpaRepository<OrderLineItem, Long> {
    // 특정 품목(item_id)에 속한 모든 주문 라인 아이템 조회
    List<OrderLineItem> findByItem_Id(Long itemId);
    
    /**
     * CompanyOrder 의 PK(orderId)를 기준으로, 해당 주문에 속한 OrderLineItem 목록을 조회합니다.
     */
    List<OrderLineItem> findByCompanyOrder_OrderId(Long orderId);
}
