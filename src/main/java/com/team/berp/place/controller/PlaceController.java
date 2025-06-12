package com.team.berp.place.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.team.berp.client.service.ClientService;
import com.team.berp.domain.Company;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.Employee;
import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.employee.dto.EmployeeDto;
import com.team.berp.employee.repository.EmployeeRepository;
import com.team.berp.item.service.ItemService;
import com.team.berp.place.dto.PlaceDTO;
import com.team.berp.place.service.PlaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // (팁) 로깅을 위해 추가하면 좋습니다.

@Slf4j // (팁) 클래스 레벨에 추가하면 log.info(), log.error() 등을 사용할 수 있습니다.
@Controller
@RequestMapping("/place")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final ClientService clientService;
    private final ItemService itemService;
    private final EmployeeRepository employeeRepository;

    /**
     * 전체 발주 목록 페이지
     * @param model
     * @return
     */
    @GetMapping
    public String placePage(Model model) {
        List<CompanyOrder> orderList = placeService.getAllOrders();
        List<Company> companies = clientService.getAllcompanies();
        List<Employee> employees = employeeRepository.findAll();
        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees);
        model.addAttribute("itemsParam", null);
        
        return "place/place";
    }

    /**
     * MRP 등 다른 페이지로부터 품목 정보를 받아 발주 페이지를 로드
     * 예: /place/purchase-order?items=RAW001:10,RAW002:5
     * @param itemsParam 품목코드:수량 형식의 문자열
     * @param model
     * @return
     */
    @GetMapping("/purchase-order")
    public String purchaseOrderPage(@RequestParam("items") String itemsParam, Model model) {
        List<CompanyOrder> orderList = placeService.getAllOrders();
        List<Company> companies = clientService.getAllcompanies();
        List<Employee> employees = employeeRepository.findAll();
        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees);
        // 뷰의 JavaScript가 이 파라미터를 사용하여 발주 모달을 자동으로 채움
        model.addAttribute("itemsParam", itemsParam);
        
        return "place/place";
    }
    
    /**
     * 입고 관리 페이지로 이동
     * @return
     */
    @GetMapping("/receive")
    public String receivePage() {
        return "receive"; // src/main/resources/templates/receive.html (가정)
    }

    // 품목 타입에 따라 제품 정보 불러오기 (Ajax)
    @GetMapping("/items")
    @ResponseBody
    public List<Item> getItemsByType(@RequestParam("type") String type) {
        ItemType itemType;
        if ("자재".equals(type)) {
            itemType = ItemType.raw;
        } else if ("완제품".equals(type)) {
            itemType = ItemType.product;
        } else {
            throw new IllegalArgumentException("잘못된 품목 유형입니다.");
        }
        return placeService.findByType(itemType);
    }

    // 담당자 정보 불러오기 (Ajax)
    @GetMapping("/employees/byCompany")
    @ResponseBody
    public ResponseEntity<EmployeeDto> getEmployeeByCompanyId(@RequestParam("companyId") Long companyId) {
        return placeService.getEmployeeByCompanyId(companyId)
            .map(EmployeeDto::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // 발주 등록 (JSON)
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> placeAdd(@RequestBody PlaceDTO dto) {
        try {
            placeService.registerOrder(dto);
            return ResponseEntity.ok("발주가 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            log.error("발주 등록 중 오류 발생", e); // System.out 대신 log.error 사용
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("발주 등록 중 오류가 발생했습니다.");
        }
    }

    // 발주 수정을 위한 데이터 조회 (모달용)
    @GetMapping("/edit/{lineItemId}")
    public ResponseEntity<PlaceDTO> getPlaceForEdit(@PathVariable("lineItemId") Long lineItemId) {
        PlaceDTO dto = placeService.getPlaceEditData(lineItemId);
        return ResponseEntity.ok(dto);
    }
    
    // 발주 수정 처리
    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<?> updatePlace(@RequestBody PlaceDTO placeDTO) {
        try {
            placeService.updateOrder(placeDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "발주 정보가 성공적으로 수정되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("발주 수정 중 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "발주 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 발주 상세 조회 (상세 모달용)
    @GetMapping("/detail/{lineItemId}")
    @ResponseBody
    public ResponseEntity<PlaceDTO> getPlaceDetail(@PathVariable("lineItemId") Long lineItemId) {
        try {
            PlaceDTO dto = placeService.getPlaceDetailData(lineItemId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("발주 상세 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 발주 검색
    @GetMapping("/search")
    public String searchPlace(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<CompanyOrder> orderList;
        if (keyword != null && !keyword.trim().isEmpty()) {
            orderList = placeService.searchOrdersByItemName(keyword.trim());
        } else {
            orderList = placeService.getAllOrders();
        }
        
        List<Company> companies = clientService.getAllcompanies();
        List<Employee> employees = employeeRepository.findAll();
        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees);
        model.addAttribute("searchKeyword", keyword);
        model.addAttribute("itemsParam", null);
        
        return "place/place";
    }

    // 발주 확정 (WAITING → CONFIRMED)
    @PostMapping("/{orderId}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmOrder(@PathVariable("orderId") Long orderId) {
        try {
            CompanyOrder updatedOrder = placeService.confirmOrder(orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "발주가 확정되었습니다.");
            response.put("newStatus", updatedOrder.getOrderStatus().name());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 입고 완료 (CONFIRMED → COMPLETED)
    @PostMapping("/{orderId}/complete")
    @ResponseBody
    public ResponseEntity<?> completeOrder(@PathVariable("orderId") Long orderId) {
        try {
            CompanyOrder updatedOrder = placeService.completeOrder(orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "입고 처리가 완료되었습니다.");
            response.put("newStatus", updatedOrder.getOrderStatus().name());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // 발주 품목 삭제 (또는 발주서 전체 삭제)
    @DeleteMapping("/item/{lineItemId}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteOrderItem(@PathVariable("lineItemId") Long lineItemId) {
        try {
            String result = placeService.deleteOrderLineItem(lineItemId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("품목 삭제 중 런타임 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("품목 삭제 중 예상치 못한 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "삭제 처리 중 시스템 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}