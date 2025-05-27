package com.team.berp.place.controller;

import com.team.berp.domain.CompanyOrder;
import com.team.berp.place.dto.PlaceDTO;
//import com.team.berp.place.service.PlaceService;

import ch.qos.logback.core.model.Model;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // @Controller + @ResponseBody (JSON 반환용)
@RequestMapping("/place")
@RequiredArgsConstructor
public class PlaceController {

//    private final PlaceService placeService;

    // GET 요청: 발주 페이지 반환
    @GetMapping
    public String placePage() {
        return "place/place";
    }

    // POST 요청: 발주 등록 처리
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
