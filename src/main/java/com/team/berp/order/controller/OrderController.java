// File: src/main/java/com/team/berp/order/controller/OrderController.java
package com.team.berp.order.controller;

import com.team.berp.order.dto.OrderPageDto;
import com.team.berp.order.service.OrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService service) {
        this.orderService = service;
    }

    /** 메인 화면: 주문 리스트 + 필터(고객사∙품목∙날짜) + 페이징(10개씩) */
    @GetMapping
    public String viewList(
        @RequestParam(value="companyName", required=false) String companyName,
        @RequestParam(value="itemName",    required=false) String itemName,
        @RequestParam(value="dateFrom",    required=false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(value="dateTo",      required=false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @PageableDefault(size = 10) Pageable pageable,
        Model model
    ) {
        // 1) 주문 페이징 데이터
        OrderPageDto page = orderService.getOrders(companyName, itemName, dateFrom, dateTo, pageable);
        model.addAttribute("page", page);

        // 2) 검색용 드롭다운 채우기
        model.addAttribute("companies", orderService.getAllCompanies());
        model.addAttribute("items",     orderService.getAllItems());

        return "order/order";
    }
    
    /** 주문 등록 모달 조각 반환 */
    @GetMapping("/fragments/registerModal")
    public String registerModalFragment() {
        // 템플릿 경로: templates/order/order-register-modal.html
        // ::registerModal 는 그 파일 안의 th:fragment="registerModal" 부분을 가리킵니다.
        return "order/order-register-modal :: registerModal";
    }

    /** 주문 수정 모달 조각 반환 */
    @GetMapping("/fragments/updateModal")
    public String updateModalFragment() {
        return "order/order-fixed           :: updateModal";
    }

    /** 주문 상세 모달 조각 반환 */
    @GetMapping("/fragments/detailModal")
    public String detailModalFragment() {
        return "order/order-detail-modal     :: detailModal";
    }
    
    
}