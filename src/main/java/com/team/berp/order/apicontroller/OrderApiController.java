// OrderApiController.java
package com.team.berp.order.apicontroller;

import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주문 관련 AJAX 요청을 처리하는 API 컨트롤러
 */
@RestController
@RequestMapping("/api/orders")
public class OrderApiController {
    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 특정 주문의 상세 정보 조회
     */
    @GetMapping("/{orderId}")
    public OrderDto getOrderDetail(@PathVariable Long orderId) {
        return orderService.getOrderDetail(orderId);
    }

    /**
     * 주문 수정 처리
     */
    @PutMapping("/{orderId}")
    public OrderDto updateOrder(
            @PathVariable Long orderId,
            @RequestBody OrderDto dto
    ) {
        dto.setOrderId(orderId);
        return orderService.updateOrder(dto);
    }
}