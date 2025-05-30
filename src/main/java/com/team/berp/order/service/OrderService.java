// OrderService.java
package com.team.berp.order.service;

import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.dto.OrderSummaryDto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 주문 관련 비즈니스 로직 인터페이스
 */
public interface OrderService {
    List<OrderDto> findByFilters(Long companyId, Long itemId, LocalDate fromDate, LocalDate toDate);
    void createOrder(OrderDto dto);
    OrderDto getOrderDetail(Long orderId);
    OrderDto updateOrder(OrderDto dto);
    void deleteOrders(List<Long> orderIds);
    Page<OrderSummaryDto> findOrderSummaries(
            Long companyId,
            Long itemId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
        );
}