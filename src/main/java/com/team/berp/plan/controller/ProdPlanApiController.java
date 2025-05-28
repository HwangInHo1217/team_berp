package com.team.berp.plan.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.domain.ProdPlan;
import com.team.berp.plan.dto.ProdPlanRequestDTO;
import com.team.berp.plan.dto.ProdPlanResponseDTO;
import com.team.berp.plan.service.ProdPlanService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProdPlanApiController {
	
	private final ProdPlanService prodPlanService;
	
	  // ✅ 전체 생산계획 목록 조회
    @GetMapping("/api/prod-plan/list")
    public ResponseEntity<List<ProdPlanResponseDTO>> getAllProdPlans() {
        return ResponseEntity.ok(prodPlanService.getAllPlans());
    }
    // ✅ 생산계획 등록
    @PostMapping("/plan/add")
    public String addProdPlan(@ModelAttribute ProdPlanRequestDTO dto) {
        prodPlanService.savePlan(dto);
        return "redirect:/plan"; // 🔁 등록 성공 후 생산계획 관리 페이지로 이동
    }

    // ✅ 생산계획 수정
    @PostMapping("/plan/update/{planId}")
    public ResponseEntity<String> updateProdPlan(@PathVariable Long planId,
                                                 @ModelAttribute ProdPlanRequestDTO dto) {
        prodPlanService.updatePlan(planId, dto); // 서비스에 수정 요청
        return ResponseEntity.ok("생산계획 수정 성공");
    }
}
