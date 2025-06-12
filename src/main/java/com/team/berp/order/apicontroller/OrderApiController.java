package com.team.berp.order.apicontroller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;
import com.team.berp.domain.ItemType;
import com.team.berp.domain.Warehouse;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.order.dto.CreateOrderRequest;
import com.team.berp.order.dto.ItemWarehouseResponse;
import com.team.berp.order.dto.OrderDetailResponse;
import com.team.berp.order.dto.OrderSummaryDto;
import com.team.berp.order.repository.Order_CompanyRepository;
import com.team.berp.order.service.OrderService;
import com.team.berp.stock.service.StockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService               orderService;
    private final Order_CompanyRepository    companyRepo;
    private final ItemRepository      itemRepo;
    private final StockService stockService;
    
 

    // --- 고객사 조회 (검색 select 채우기) ---
    @GetMapping("/companies")
    public List<CompanyListDto> listCompanies() {
        return companyRepo.findByCompanyType(CompanyType.CUSTOMER).stream()
            .map(c -> new CompanyListDto(
                   c.getCompanyId(),
                   c.getCompanyName()
                 ))
            .collect(Collectors.toList());
    }

    // --- 고객사 상세 정보 (담당자, 거래처담당자) ---
    @GetMapping("/companies/{companyId}/info")
    public CompanyInfoDto getCompanyInfo(@PathVariable("companyId") Long companyId) {
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
        return itemRepo.findByType(ItemType.product).stream()
            .map(i -> new ItemDto(
                   i.getId(),
                   i.getName(),
                   i.getUnit(),
                   i.getItemPrice()
                 ))
            .collect(Collectors.toList());
    }

    // --- 주문 등록 ---
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            orderService.createOrder(request); // ✅ 올바른 타입으로 넘김
            return ResponseEntity.ok("주문 등록 성공");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("저장 중 오류 발생: " + e.getMessage());
        }
    }


    // --- 주문 상세 조회 ---
    @GetMapping("/orders/{orderId}")
    public OrderDetailResponse getOrder(@PathVariable("orderId") Long orderId) {
        return orderService.getOrderDetail(orderId);
    }

 // 예: 쿼리 파라미터로 전달받는 경우
    @DeleteMapping("/orders")
    
    public ResponseEntity<?> deleteOrders(@RequestBody List<Long> orderIds) {
        try {
            orderService.deleteOrders(orderIds);
            return ResponseEntity.ok("삭제 완료");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }



    // DTO for /api/companies
    public static record CompanyListDto(Long companyId, String companyName) {}

    // DTO for /api/companies/{id}/info
    public static record CompanyInfoDto(String empName, String companyEmpName) {}

    // DTO for /api/items
    public static record ItemDto(Long id, String itemName, String unit, Long itemPrice) {}

    
 // 주문 목록 조회 (비동기용)
    @GetMapping("/orders/list")
    public Page<OrderSummaryDto> getOrdersList(
        @RequestParam(name = "companyId", required = false) Long companyId,
        @RequestParam(name = "itemId", required = false) Long itemId,
        @RequestParam(name = "fromDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return orderService.findOrderSummaries(companyId, itemId, fromDate, toDate, pageable);
    }
    
    /**
     * ✅ 특정 주문(orderId)에 속한 아이템들이 어느 창고에 재고가 있는지 조회
     * - key: itemId
     * - value: 창고 목록 (창고명 + 수량)
     *
     * @param orderId 주문 ID
     * @return itemId별 창고 재고 정보 Map
     */
    @GetMapping("/orders/{orderId}/shipment-info")
    public List<ItemWarehouseResponse> getShipmentInfo(@PathVariable("orderId") Long orderId)  {
    	System.out.println("컨트롤러연결확인");
        // 서비스 호출: 주문에 속한 품목들에 대해 재고 있는 창고 정보 조회
    	return orderService.getShipmentInfoList(orderId);
    }

    
 // --- 주문 수정 ---
    @PutMapping("/orders/{orderId}")
    public ResponseEntity<?> updateOrder(
        @PathVariable("orderId") Long orderId,
        @RequestBody CreateOrderRequest request
    ) {
        try {
            orderService.updateOrder(orderId, request);
            return ResponseEntity.ok("주문 수정 성공");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("수정 중 오류 발생: " + e.getMessage());
        }
    }

}