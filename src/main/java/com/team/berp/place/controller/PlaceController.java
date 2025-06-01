package com.team.berp.place.controller;


import com.team.berp.client.service.ClientService;
import com.team.berp.domain.Company;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.Employee;
import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.employee.repository.EmployeeRepository;
import com.team.berp.item.service.ItemService;
import com.team.berp.place.dto.PlaceDTO;
//import com.team.berp.place.service.PlaceService;
import com.team.berp.place.service.PlaceService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
//        List<Item> items = itemService.getAllItems(); // 품목 리스트 조회

        
        model.addAttribute("orderList", orderList);
        model.addAttribute("companies", companies);
        model.addAttribute("employees", employees); // 직원 리스트 모델에 담기
//        model.addAttribute("items", items); // 여기가 핵심

        return "place/place";
    }
    
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

    
    //직원 아이디로 상세 정보 조회
    @GetMapping("/employee/{employeeId}")
    @ResponseBody
    public ResponseEntity<Employee> getEmployeeById(@PathVariable("employeeId") Long employeeId) {
        return employeeRepository.findById(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    
    @PostMapping("/add")
    public String placeAdd(@ModelAttribute PlaceDTO dto) {
    	placeService.registerOrder(dto);
    	return "redirect:/place";
    }
    

 
}
