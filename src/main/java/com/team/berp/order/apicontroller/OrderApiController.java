// File: src/main/java/com/team/berp/order/apicontroller/OrderApiController.java
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

    /** 1) 주문 목록 조회 (필터 + 페이징) */
    @GetMapping
    public OrderPageDto getList(
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

    /** 2) 단일 주문 상세 조회 */
    @GetMapping("/{orderNum}")
    public OrderDto getDetail(@PathVariable("orderNum") Long orderNum) {
        return orderService.getOrder(orderNum);
    }

    /** 3) 주문 등록 (항상 CUSTOMER 타입) */
    @PostMapping
    public OrderDto create(@RequestBody OrderRegisterFormDto form) {
        return orderService.registerOrder(form);
    }

    /** 4) 주문 수정 */
    @PutMapping("/{orderNum}")
    public OrderDto modify(
        @PathVariable("orderNum") Long orderNum,
        @RequestBody OrderRegisterFormDto form
    ) {
        return orderService.updateOrder(orderNum, form);
    }

    /** 5) 주문 삭제 (여러 건) */
    @DeleteMapping
    public void remove(@RequestBody List<Long> orderNums) {
        orderService.deleteOrders(orderNums);
    }

    /** 6) 고객사 → 담당자 자동 채움용 */
    @GetMapping("/company/{companyName}")
    public CompanyContactDto getCompanyContact(
        @PathVariable("companyName") String companyName
    ) {
        return orderService.getCompanyContactInfo(companyName);
    }

}