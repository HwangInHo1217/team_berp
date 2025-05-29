package com.team.berp.order.controller;

import com.team.berp.order.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 뷰(View) 반환 컨트롤러: Thymeleaf 템플릿 렌더링
 */
@Controller
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 메인 페이지 렌더링 */
    @GetMapping("/order")
    public String orderPage(Model model) {
        model.addAttribute("companies", orderService.getAllCompanies());
        model.addAttribute("items",    orderService.getAllItems());
        return "order/order";
    }
}
