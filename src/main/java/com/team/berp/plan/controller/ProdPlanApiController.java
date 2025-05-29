package com.team.berp.plan.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.bom.dto.ItemSelectionDTO;
import com.team.berp.bom.service.BomService;
import com.team.berp.domain.Item;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.plan.dto.ProdPlanRequestDTO;
import com.team.berp.plan.dto.ProdPlanResponseDTO;
import com.team.berp.plan.service.ProdPlanService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProdPlanApiController {

	private final ProdPlanService prodPlanService;
	private final BomService bomService;
	// ✅ 전체 생산계획 목록 조회
	@GetMapping("/api/prod-plan/list")
	public ResponseEntity<?> getAllProdPlans(    // 🔧 파라미터 이름을 명시적으로 지정 (Spring이 리플렉션으로 못 읽는 경우 대비)
		    @RequestParam(name = "keyword", required = false) String keyword, 
		    @RequestParam(name = "startDate", required = false) 
		        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, 
		    @RequestParam(name = "endDate", required = false) 
		        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
		    @RequestParam(name = "page", defaultValue = "0") int page,
		    @RequestParam(name = "size", defaultValue = "10") int size,
		    @RequestParam(name="status", required = false) String status)  // 페이지 당 항목 수)
	{
		// 페이지 정보 생성 (최신순)
	    Pageable pageable = PageRequest.of(page, size, Sort.by("planDate").descending());

	    // 서비스에서 검색 조건과 페이징을 적용한 결과 조회
	    Page<ProdPlanResponseDTO> result = prodPlanService.searchPlans(keyword, startDate, endDate,status, pageable);
		return ResponseEntity.ok(result);
	}

	// ✅ 생산계획 등록
	@PostMapping("/plan/add")
	public ResponseEntity<String> addProdPlan(@RequestBody  ProdPlanRequestDTO dto) {
		prodPlanService.savePlan(dto);
		return ResponseEntity.ok("생산계획 등록 성공");// 🔁 등록 성공 후 생산계획 관리 페이지로 이동
	}

	// ✅ 생산계획 수정
	@PutMapping("/api/prod-plan/update/{planId}")
	public ResponseEntity<String> updateProdPlan(
			@PathVariable("planId") Long planId, 
			@RequestBody ProdPlanRequestDTO dto) {
		prodPlanService.updatePlan(planId, dto); // 서비스에 수정 요청
		return ResponseEntity.ok("생산계획 수정 성공");
	}
	// ✅ 삭제 기능 추가
	@DeleteMapping("/api/prod-plan/delete/{planId}")
	public ResponseEntity<?> deletePlan(@PathVariable("planId") Long planId) {
	    prodPlanService.deletePlan(planId);
	    return ResponseEntity.ok("삭제 완료");
	}
	@GetMapping("/api/item/all-products")//selction 
	public ResponseEntity<ItemSelectionDTO> getAllProductItems() {
		ItemSelectionDTO products = bomService.getSelectableItems();
	    return ResponseEntity.ok(products);
	}

}
