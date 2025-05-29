package com.team.berp.plan.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.domain.Item;
import com.team.berp.domain.ProdPlan;
import com.team.berp.domain.ProdPlan.PlanStatus;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.plan.dto.ProdPlanRequestDTO;
import com.team.berp.plan.dto.ProdPlanResponseDTO;
import com.team.berp.plan.repository.ProdPlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdPlanService {

	private final ItemRepository itemRepository;

	private final ProdPlanRepository prodPlanRepository;
	
	
	public void deletePlan(Long planId) {
	    if (!prodPlanRepository.existsById(planId)) {
	        throw new IllegalArgumentException("존재하지 않는 생산계획입니다.");
	    }
	    prodPlanRepository.deleteById(planId);
	}


	public Page<ProdPlanResponseDTO> searchPlans(String keyword, LocalDate startDate, LocalDate endDate, String status,
			Pageable pageable) {
		// ✅ 전체 데이터 조회
		List<ProdPlan> allPlans = prodPlanRepository.findAll();

		// ✅ 조건에 맞는 결과만 필터링
		List<ProdPlanResponseDTO> filteredList = new ArrayList<>();

		for (ProdPlan plan : allPlans) {
			boolean match = true;

			// 🔍 keyword 필터 (품목명 또는 코드 포함)
			if (keyword != null && !keyword.isEmpty()) {
				String code = plan.getItem().getCode();
				String name = plan.getItem().getName();
				if ((code == null || !code.contains(keyword)) && (name == null || !name.contains(keyword))) {
					match = false;
				}
			}

			// 🔍 시작일 필터
			if (startDate != null && plan.getPlanDate() != null && plan.getPlanDate().isBefore(startDate)) {
				match = false;
			}

			// 🔍 종료일 필터
			if (endDate != null && plan.getPlanDate() != null && plan.getPlanDate().isAfter(endDate)) {
				match = false;
			}
			  // ✅ 상태 필터 추가
	        if (status != null && !status.isEmpty()) {
	        	try {
	                PlanStatus statusEnum = PlanStatus.valueOf(status);
	                if (plan.getStatus() != statusEnum) {
	                    match = false;
	                }
	            } catch (IllegalArgumentException e) {
	                match = false; // 유효하지 않은 status 값은 매칭 제외
	            }
	        }

			// ✅ 조건에 부합하면 DTO로 변환하여 추가
			if (match) {
				filteredList.add(new ProdPlanResponseDTO(plan));
			}
		}

		// ✅ 페이징 적용: 시작 인덱스, 종료 인덱스 계산
		int start = (int) pageable.getOffset(); // 예: page 1, size 10이면 10
		int end = Math.min(start + pageable.getPageSize(), filteredList.size()); // 최대 범위 초과 방지

		// ✅ 부분 리스트 생성
		List<ProdPlanResponseDTO> pageContent = new ArrayList<>();
		for (int i = start; i < end; i++) {
			pageContent.add(filteredList.get(i));
		}

		// ✅ PageImpl로 리턴 (전체 개수는 필터링된 전체 개수 기준)
		return new PageImpl<>(pageContent, pageable, filteredList.size());
	}

	/*
	 * // 전체 생산계획을 조회하여 DTO로 반환하는 메서드 public List<ProdPlanResponseDTO> getAllPlans()
	 * {
	 * 
	 * // 결과를 담을 DTO 리스트 선언 List<ProdPlanResponseDTO> resultList = new
	 * ArrayList<>();
	 * 
	 * // prod_plan 테이블의 모든 엔티티 조회 List<ProdPlan> plans =
	 * prodPlanRepository.findAll();
	 * 
	 * // 각 엔티티를 DTO로 변환하여 리스트에 추가 for (ProdPlan plan : plans) { Item
	 * item=plan.getItem(); ProdPlanResponseDTO dto = ProdPlanResponseDTO.builder()
	 * .planId(plan.getPlanId()) // 계획 ID .itemCode(item.getCode())
	 * .itemName(item.getName()) .planQty(plan.getPlanQty()) // 계획 수량
	 * .unit(plan.getUnit()) // 단위 .planDate(plan.getPlanDate()) // 계획일
	 * .dueDate(plan.getDueDate()) // 납기일 .startDate(plan.getStartDate()) // 생산시작
	 * 예정일 .status(plan.getStatus()) // 상태 (PLANNED 등) .priority(plan.getPriority())
	 * // 우선순위 .manager(plan.getManager()) // 담당자 .remark(plan.getRemark()) // 비고
	 * .build();
	 * 
	 * resultList.add(dto); // 변환된 DTO를 리스트에 추가 }
	 * 
	 * // 최종 결과 리스트 반환 return resultList; }
	 */

	// ✅ 등록
	public void savePlan(ProdPlanRequestDTO dto) {
		Item item = itemRepository.findById(dto.getItem_id())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품목"));

		ProdPlan plan = ProdPlan.builder().item(item).planQty(dto.getQuantity()).unit(dto.getUnit())
				.planDate(LocalDate.parse(dto.getPlan_date())).dueDate(LocalDate.parse(dto.getDue_date()))
				.status(dto.getStatus()).manager(dto.getManager()).remark(dto.getNote()).build();

		prodPlanRepository.save(plan);
	}

	// ✅ 수정
	@Transactional
	public void updatePlan(Long planId, ProdPlanRequestDTO dto) {
		ProdPlan plan = prodPlanRepository.findById(planId).orElseThrow(() -> new IllegalArgumentException("계획 없음"));

		Item item = itemRepository.findById(dto.getItem_id()).orElseThrow(() -> new IllegalArgumentException("품목 없음"));

		plan.update(item, dto.getQuantity(), dto.getUnit(), LocalDate.parse(dto.getPlan_date()),
				LocalDate.parse(dto.getDue_date()), dto.getStatus(), dto.getManager(), dto.getNote());
	}
}
