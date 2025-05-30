package com.team.berp.order.apicontroller;

import com.team.berp.domain.Company;
import com.team.berp.domain.Item;
import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.service.OrderService;
import com.team.berp.order.repository.Order_CompanyRepository;
import com.team.berp.order.repository.Order_ItemRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OrderApiController {

    private final OrderService               orderService;
    private final Order_CompanyRepository    companyRepo;
    private final Order_ItemRepository       itemRepo;

    public OrderApiController(
        OrderService orderService,
        Order_CompanyRepository companyRepo,
        Order_ItemRepository itemRepo
    ) {
        this.orderService = orderService;
        this.companyRepo  = companyRepo;
        this.itemRepo     = itemRepo;
    }

    // --- 고객사 조회 (검색 select 채우기) ---
    @GetMapping("/companies")
    public List<CompanyListDto> listCompanies() {
        return companyRepo.findAll().stream()
            .map(c -> new CompanyListDto(
                   c.getCompanyId(),
                   c.getCompanyName()
                 ))
            .collect(Collectors.toList());
    }

    // --- 고객사 상세 정보 (담당자, 거래처담당자) ---
    @GetMapping("/companies/{companyId}/info")
    public CompanyInfoDto getCompanyInfo(@PathVariable Long companyId) {
        Company c = companyRepo.findById(companyId)
                     .orElseThrow(() -> new IllegalArgumentException("고객사를 찾을 수 없습니다."));
        return new CompanyInfoDto(
            c.getEmployee().getEmpName(),
            c.getCompanyEmpName()
        );
    }

    // --- 품목 조회 (등록·수정 modal 용 select 채우기) ---
    @GetMapping("/items")
    public List<ItemDto> listItems() {
        return itemRepo.findAll().stream()
            .map(i -> new ItemDto(
                   i.getId(),
                   i.getName(),
                   i.getUnit()
                 ))
            .collect(Collectors.toList());
    }

    // --- 주문 등록 ---
    @PostMapping("/orders")
    public ResponseEntity<Void> createOrder(@ModelAttribute OrderDto dto) {
        orderService.createOrder(dto);  // 단일 트랜잭션 안에서 order + lineItems 저장 :contentReference[oaicite:1]{index=1}
        return ResponseEntity.ok().build();
    }

    // --- 주문 상세 조회 ---
    @GetMapping("/orders/{orderId}")
    public OrderDto getOrder(@PathVariable Long orderId) {
        return orderService.getOrderDetail(orderId);
    }

    // --- 주문 수정 ---
    @PutMapping("/orders")
    public OrderDto updateOrder(@ModelAttribute OrderDto dto) {
        return orderService.updateOrder(dto);
    }

    // --- 주문 삭제 (다중) ---
    @DeleteMapping("/orders")
    public ResponseEntity<Void> deleteOrders(@RequestParam List<Long> ids) {
        orderService.deleteOrders(ids);
        return ResponseEntity.ok().build();
    }

    // DTO for /api/companies
    public static record CompanyListDto(Long companyId, String companyName) {}

    // DTO for /api/companies/{id}/info
    public static record CompanyInfoDto(String empName, String companyEmpName) {}

    // DTO for /api/items
    public static record ItemDto(Long id, String itemName, String unit) {}
}