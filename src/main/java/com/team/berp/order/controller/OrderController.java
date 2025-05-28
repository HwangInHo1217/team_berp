package com.example.order.controller;

import com.example.order.dto.OrderDto;
import com.example.order.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;

@Controller
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 주문 관리 페이지
     * - 초기 로딩 시 모든 주문을 조회하여 모델에 담아 Thymeleaf로 렌더링
     * - 고객사, 품목 필터 옵션도 함께 전달
     */
    @GetMapping("/orders")
    public String listPage(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            Model model) {
        // 필터링된 주문 목록 조회
        List<OrderDto> orders = orderService.getOrders(companyName, itemName, dateFrom, dateTo);
        model.addAttribute("orders", orders);
        
        // 필터 옵션 전달: 고객사, 품목
        model.addAttribute("companies", orderService.getAllCompanies());
        model.addAttribute("items", orderService.getAllItems());
        
        return "order";  // Thymeleaf 템플릿 order.html 사용
    }
}