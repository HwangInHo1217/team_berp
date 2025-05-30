// OrderController.java
package com.team.berp.order.controller;

import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 페이지를 렌더링하고 조회 필터를 처리하는 컨트롤러
 */
@Controller
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 주문 리스트 페이지 조회
     */
    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model
    ) {
        List<OrderDto> orders = orderService.findByFilters(companyId, itemId, fromDate, toDate);
        model.addAttribute("orders", orders);
        return "order"; // Thymeleaf 템플릿 order.html
    }

    /**
     * 주문 등록 폼에서 입력된 데이터를 처리
     */
    @PostMapping("/orders/register")
    public String registerOrder(@ModelAttribute OrderDto dto) {
        orderService.createOrder(dto);
        return "redirect:/orders";
    }

    /**
     * 주문 개별 삭제 (체크박스로 여러 건 삭제 가능)
     */
    @PostMapping("/orders/delete")
    public String deleteOrders(@RequestParam List<Long> orderIds) {
        orderService.deleteOrders(orderIds);
        return "redirect:/orders";
    }
}