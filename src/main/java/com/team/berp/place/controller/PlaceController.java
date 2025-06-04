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
//import com.team.berp.place.service.PlaceService;
import com.team.berp.place.service.PlaceService;

import lombok.RequiredArgsConstructor;

@Controller //@RestController는 문자열을 그대로 반환

@RequestMapping("/place")
//@ResponseBody
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final ClientService clientService;
    private final ItemService itemService;
    private final EmployeeRepository employeeRepository;

    
    @GetMapping
    public String placePage(Model model) {
        List<CompanyOrder> orderList = placeService.getAllOrders();
        List<Company> companies = clientService.getAllcompanies();
        List<Employee> employees = employeeRepository.findAll(); // 직원 전체 조회
        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees); // 직원 리스트 모델에 담기

        return "place/place";
    }
    
    //입고 페이지 이동
    @GetMapping("/receive")
    public String receivePage() {
        return "receive";  // 예: receive.html 또는 receive.jsp로 포워딩
    }

    
    //제품 정보 불러오기
    @GetMapping("/items")
    @ResponseBody
    public List<Item> getItemsByType(@RequestParam("type") String type) {
        // "자재" or "완제품" → enum으로 변환
        ItemType itemType;

        if ("자재".equals(type)) {
            itemType = ItemType.raw; //MAT
        } else if ("완제품".equals(type)) {
            itemType = ItemType.product; //PRD
        } else {
            throw new IllegalArgumentException("잘못된 품목 유형입니다.");
        }

        return placeService.findByType(itemType);
    }

    //담당자 정보 불러오기
    @GetMapping("/employees/byCompany")
    @ResponseBody
    public ResponseEntity<EmployeeDto> getEmployeeByCompanyId(@RequestParam("companyId") Long companyId) {
    	System.out.println("📌 컨트롤러 진입: companyId = " + companyId); // 로그 확인용
    	return placeService.getEmployeeByCompanyId(companyId)
            .map(EmployeeDto::fromEntity) // Employee → EmployeeDto 변환
            .map(ResponseEntity::ok)      // EmployeeDto → ResponseEntity.ok(...)
            .orElse(ResponseEntity.notFound().build());
    }

    //발주 등록
//    @PostMapping("/add")
//    public String placeAdd(@ModelAttribute PlaceDTO dto) {
//    	placeService.registerOrder(dto);
//    	return "redirect:/place";
//    }
  //발주 등록 - JSON 방식으로 수정
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> placeAdd(@RequestBody PlaceDTO dto) {
        try {
            placeService.registerOrder(dto);
            return ResponseEntity.ok("발주가 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("발주 등록 중 오류가 발생했습니다.");
        }
    }

    //발주 수정
    @GetMapping("/edit/{lineItemId}")
    public ResponseEntity<PlaceDTO> getPlaceForEdit(@PathVariable("lineItemId") Long lineItemId) {
        PlaceDTO dto = placeService.getPlaceEditData(lineItemId);
        System.out.println(dto.getOrderQty());
        return ResponseEntity.ok(dto);
    }
    
    //발주 수정 등록
//    @PostMapping("/update")
//    @ResponseBody
//    public ResponseEntity<?> updatePlace(@RequestBody PlaceDTO placeDTO) {
//        try {
//            placeService.updateOrder(placeDTO);
//            return ResponseEntity.ok("발주 정보가 성공적으로 수정되었습니다.");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("발주 수정 중 오류가 발생했습니다.");
//        }
//    }

  //발주 수정 등록
//    @PostMapping("/update")
//    @ResponseBody
//    public ResponseEntity<?> updatePlace(@RequestBody PlaceDTO placeDTO) {
//        try {
//            System.out.println("✅ 수정 요청 받음 - orderId: " + placeDTO.getOrderId());
//            System.out.println("✅ lineItems 개수: " + (placeDTO.getLineItems() != null ? placeDTO.getLineItems().size() : 0));
//            
//            placeService.updateOrder(placeDTO);
//            
//            System.out.println("✅ 수정 완료");
//            return ResponseEntity.ok("발주 정보가 성공적으로 수정되었습니다.");
//        } catch (Exception e) {
//            System.err.println("❌ 발주 수정 컨트롤러 오류: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                               .body("발주 수정 중 오류가 발생했습니다: " + e.getMessage());
//        }
//    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<?> updatePlace(@RequestBody PlaceDTO placeDTO) {
        try {
            System.out.println("✅ 수정 요청 받음 - orderId: " + placeDTO.getOrderId());
            System.out.println("✅ lineItems 개수: " + (placeDTO.getLineItems() != null ? placeDTO.getLineItems().size() : 0));
            
            placeService.updateOrder(placeDTO);
            
            System.out.println("✅ 수정 완료");
            
            // ✅ JSON 객체로 응답 (Map 사용)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "발주 정보가 성공적으로 수정되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ 발주 수정 컨트롤러 오류: " + e.getMessage());
            e.printStackTrace();
            
            // ✅ 에러도 JSON 객체로 응답
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "발주 수정 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
  //발주 상세 조회 (상세 모달용)
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
    
  //발주 검색
    @GetMapping("/search")
    public String searchPlace(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<CompanyOrder> orderList;
        List<Company> companies = clientService.getAllcompanies();
        List<Employee> employees = employeeRepository.findAll();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 검색어가 있으면 검색 실행
            orderList = placeService.searchOrdersByItemName(keyword.trim());
            System.out.println("✅ 검색 실행 - 키워드: '" + keyword + "', 결과 개수: " + orderList.size());
        } else {
            // 검색어가 없으면 전체 조회
            orderList = placeService.getAllOrders();
        }
        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees);
        model.addAttribute("searchKeyword", keyword); // 검색어 유지용
        
        return "place/place";
    }
    
}
