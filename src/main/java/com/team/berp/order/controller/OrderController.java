package com.team.berp.order.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.team.berp.domain.Company.CompanyType;
import com.team.berp.order.dto.OrderSummaryDto;
import com.team.berp.order.repository.Order_CompanyOrderRepository;
import com.team.berp.order.repository.Order_CompanyRepository;
import com.team.berp.order.repository.Order_ItemRepository;
import com.team.berp.order.service.OrderService;

@Controller
@RequestMapping("/order")
public class OrderController {
	private final OrderService                 orderService;  // 추가
    private final Order_CompanyOrderRepository orderRepo;
    private final Order_CompanyRepository     companyRepo;
    private final Order_ItemRepository        itemRepo;

    public OrderController(
    	OrderService orderService,                 // 생성자 인자에 추가
        Order_CompanyOrderRepository orderRepo,
        Order_CompanyRepository companyRepo,
        Order_ItemRepository itemRepo
    ) {
    	this.orderService = orderService;          // 필드에 할당
        this.orderRepo   = orderRepo;
        this.companyRepo = companyRepo;
        this.itemRepo    = itemRepo;
    }

    /**
     * 주문 목록 페이지
     * - 페이징: 10개씩
     * - 필터: 고객사(companyId), 품목(itemId), 날짜(fromDate/toDate)
     * - 검색용 select 채우기
     */
    @GetMapping
    public String listOrders(
        @RequestParam(name = "companyId", required = false) Long companyId,
        @RequestParam(name = "itemId", required = false) Long itemId,
        @RequestParam(name = "fromDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,
        @RequestParam(name = "toDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,
        @PageableDefault(size = 10) Pageable pageable,
        Model model
    ) {
    	

        
        model.addAttribute("companies", companyRepo.findByCompanyType(CompanyType.CUSTOMER));
        model.addAttribute("items",     itemRepo.findAll());
        return "order/order";
    }
}