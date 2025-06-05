// File: src/main/java/com/team/berp/place/controller/PlaceController.java
package com.team.berp.place.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

@Controller
@RequestMapping("/place")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final ClientService clientService;
    private final ItemService itemService;
    private final EmployeeRepository employeeRepository;

    /**
     * (기존) 전체 발주 페이지
     * /place
     */
    @GetMapping
    public String placePage(Model model) {
        List<CompanyOrder> orderList = placeService.getAllOrders();
        List<Company> companies = clientService.getAllcompanies();
        List<Employee> employees = employeeRepository.findAll();
        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees);
        // itemsParam은 null이므로 그냥 뷰가 로드됨
        model.addAttribute("itemsParam", null);
        
        return "place/place";
    }

    /**
     * (추가) /purchase-order?items=RAW001:4470,RAW002:8940,...
     * MRP에서 넘어오는 items 파라미터(코드:수량)를 읽어서 뷰에 전달
     */
    @GetMapping("/purchase-order")
    public String purchaseOrderPage(
            @RequestParam("items") String itemsParam,
            Model model) {

        List<CompanyOrder> orderList = placeService.getAllOrders();
        List<Company> companies = clientService.getAllcompanies();
        List<Employee> employees = employeeRepository.findAll();
        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees);
        // 뷰에서 JS를 통해 itemsParam을 사용해서 모달을 띄우게 함
        model.addAttribute("itemsParam", itemsParam);
        
        return "place/place";
    }

    // 입고 페이지 이동
    @GetMapping("/receive")
    public String receivePage() {
        return "receive";
    }

    // 제품 정보 불러오기 (Ajax)
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

    // 발주 등록 - JSON 방식
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> placeAdd(@RequestBody PlaceDTO dto) {
        try {
            placeService.registerOrder(dto);
            return ResponseEntity.ok("발주가 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("발주 등록 중 오류가 발생했습니다.");
        }
    }

    // 발주 수정 - detail View (모달용 데이터 반환)
    @GetMapping("/edit/{lineItemId}")
    public ResponseEntity<PlaceDTO> getPlaceForEdit(@PathVariable("lineItemId") Long lineItemId) {
        PlaceDTO dto = placeService.getPlaceEditData(lineItemId);
        return ResponseEntity.ok(dto);
    }

    // 발주 수정 등록
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
            e.printStackTrace();
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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 발주 검색
    @GetMapping("/search")
    public String searchPlace(@RequestParam(value = "keyword", required = false) String keyword,
                              Model model) {
        List<CompanyOrder> orderList;
        List<Company> companies = clientService.getAllcompanies();
        List<Employee> employees = employeeRepository.findAll();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            orderList = placeService.searchOrdersByItemName(keyword.trim());
        } else {
            orderList = placeService.getAllOrders();
        }
        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees);
        model.addAttribute("searchKeyword", keyword);
        model.addAttribute("itemsParam", null);
        
        return "place/place";
    }
}
