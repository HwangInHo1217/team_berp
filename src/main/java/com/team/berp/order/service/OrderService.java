// OrderService.java
package com.team.berp.order.service;

import com.team.berp.order.dto.OrderDto;
import java.time.LocalDate;
import java.util.List;

/**
 * 주문 관련 비즈니스 로직 인터페이스
 */
public interface OrderService {
    List<OrderDto> findByFilters(Long companyId, Long itemId, LocalDate fromDate, LocalDate toDate);
    void createOrder(OrderDto dto);
    OrderDto getOrderDetail(Long orderId);
    OrderDto updateOrder(OrderDto dto);
    void deleteOrders(List<Long> orderIds);
}