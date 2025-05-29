package com.team.berp.order.apicontroller;

import com.team.berp.order.dto.*;
import com.team.berp.order.service.OrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 주문 목록 (dueDate 제거) */
    @GetMapping
    public OrderPageDto list(
        @RequestParam(value="companyName", required=false) String companyName,
        @RequestParam(value="itemName",    required=false) String itemName,
        @RequestParam(value="dateFrom",    required=false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(value="dateTo",      required=false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        Pageable pageable
    ) {
        return orderService.getOrders(companyName, itemName, dateFrom, dateTo, pageable);
    }

    @GetMapping("/{orderNum}")
    public OrderDto detail(@PathVariable Long orderNum) {
        return orderService.getOrder(orderNum);
    }

    @PostMapping
    public OrderDto create(@RequestBody OrderRegisterFormDto form) {
        return orderService.registerOrder(form);
    }

    @PutMapping("/{orderNum}")
    public OrderDto modify(@PathVariable Long orderNum,
                           @RequestBody OrderRegisterFormDto form) {
        return orderService.updateOrder(orderNum, form);
    }

    @DeleteMapping
    public void remove(@RequestBody List<Long> orderNums) {
        orderService.deleteOrders(orderNums);
    }

    /** 고객사 담당자 조회 (ID 기준) */
    @GetMapping("/company/{customerId}")
    public CompanyContactDto getContact(@PathVariable Long customerId) {
        return orderService.getCompanyContactInfo(customerId);
    }
}
