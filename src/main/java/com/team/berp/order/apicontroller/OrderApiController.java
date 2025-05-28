package com.example.order.controller;

import com.example.order.dto.OrderDto;
import com.example.order.dto.OrderRegisterFormDto;
import com.example.order.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {
    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 주문 목록 조회 (필터링 가능)
     */
    @GetMapping
    public List<OrderDto> list(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return orderService.getOrders(companyName, itemName, dateFrom, dateTo);
    }

    /**
     * 단일 주문 상세 조회
     */
    @GetMapping("/{orderNum}")
    public OrderDto detail(@PathVariable String orderNum) {
        return orderService.getOrder(orderNum);
    }

    /**
     * 주문 등록
     */
    @PostMapping
    public OrderDto create(@RequestBody OrderRegisterFormDto form) {
        return orderService.registerOrder(form);
    }

    /**
     * 주문 수정
     */
    @PutMapping("/{orderNum}")
    public OrderDto update(@PathVariable String orderNum,
                           @RequestBody OrderRegisterFormDto form) {
        return orderService.updateOrder(orderNum, form);
    }

    /**
     * 다중 주문 삭제
     */
    @DeleteMapping
    public void delete(@RequestBody List<String> orderNums) {
        orderService.deleteOrders(orderNums);
    }

    /**
     * 고객사 선택 시 담당자 및 거래처담당자 정보 제공
     */
    @GetMapping("/company/{companyName}")
    public Map<String, String> companyInfo(@PathVariable String companyName) {
        return orderService.getCompanyContactInfo(companyName);
    }
}