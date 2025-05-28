package com.team.berp.place.controller;

import com.team.berp.domain.CompanyOrder;
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
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    // GET 요청: 발주 페이지 반환
    @GetMapping
    public String placePage(Model m) {
    	List<CompanyOrder> orderList = placeService.getAllOrders();
        m.addAttribute("orderList", orderList);
        return "place/place";
    }
    
    @PostMapping("/add")
    public String placeAdd(@ModelAttribute PlaceDTO dto) {
    	placeService.registerOrder(dto);
    	return "redirect:/place";
    }
    

    // POST 요청: 발주 등록 처리, api 방식
//    @PostMapping
//    public ResponseEntity<?> registerOrder(@RequestBody PlaceDTO dto) {
//        try {
//            CompanyOrder savedOrder = placeService.registerOrder(dto);
//            return ResponseEntity.ok(savedOrder); // 등록된 발주 정보 반환
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("발주 등록 실패: " + e.getMessage());
//        }
//    }
    
//    @GetMapping("/list")
//    public String showPlaceList(Model model) {
//        List<CompanyOrder> orderList = placeService.getAllOrders(); // JOIN FETCH로 조회
//        model.addAttribute("orderList", orderList);
//        return "place/place"; // 위에서 보여준 HTML
//    }

    
}
