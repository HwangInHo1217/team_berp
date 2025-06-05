package com.team.berp.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.OrderLineItem;

public interface Order_OrderLineItemRepository extends JpaRepository<OrderLineItem, Long> {
	List<OrderLineItem> findByCompanyOrderOrderIdIn(List<Long> orderIds);
	
    /**
     * ✅ 특정 주문(orderId)에 속한 모든 주문 상세(OrderLineItem)를 조회
     * - companyOrder.orderId가 외래키로 매핑되어 있으므로 해당 필드로 검색 가능
     *
     * @param orderId 주문 ID
     * @return 해당 주문에 속한 OrderLineItem 리스트
     */
   List<OrderLineItem> findByCompanyOrder_OrderId(Long orderId);
	

}
