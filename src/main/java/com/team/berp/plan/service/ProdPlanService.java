package com.team.berp.plan.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.team.berp.domain.Item;
import com.team.berp.domain.ProdPlan;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.plan.dto.ProdPlanRequestDTO;
import com.team.berp.plan.dto.ProdPlanResponseDTO;
import com.team.berp.plan.repository.ProdPlanRepository;
import com.team.berp.plant.controller.plant_controller;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdPlanService {
	
	private final ItemRepository itemRepository;
    
	private final ProdPlanRepository prodPlanRepository;
    // 전체 생산계획을 조회하여 DTO로 반환하는 메서드
    public List<ProdPlanResponseDTO> getAllPlans() {

        // 결과를 담을 DTO 리스트 선언
        List<ProdPlanResponseDTO> resultList = new ArrayList<>();

        // prod_plan 테이블의 모든 엔티티 조회
        List<ProdPlan> plans = prodPlanRepository.findAll();

        // 각 엔티티를 DTO로 변환하여 리스트에 추가
        for (ProdPlan plan : plans) {
        	Item item=plan.getItem();
            ProdPlanResponseDTO dto = ProdPlanResponseDTO.builder()
                    .planId(plan.getPlanId())           // 계획 ID
                    .itemCode(item.getCode())
                    .itemName(item.getName())
                    .planQty(plan.getPlanQty())         // 계획 수량
                    .unit(plan.getUnit())               // 단위
                    .planDate(plan.getPlanDate())       // 계획일
                    .dueDate(plan.getDueDate())         // 납기일
                    .startDate(plan.getStartDate())     // 생산시작 예정일
                    .status(plan.getStatus())           // 상태 (PLANNED 등)
                    .priority(plan.getPriority())       // 우선순위
                    .manager(plan.getManager())         // 담당자
                    .remark(plan.getRemark())           // 비고
                    .build();

            resultList.add(dto); // 변환된 DTO를 리스트에 추가
        }

        // 최종 결과 리스트 반환
        return resultList;
    }
    
 // ✅ 등록
    public void savePlan(ProdPlanRequestDTO dto) {
        Item item = itemRepository.findById(dto.getItem_id())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품목"));

        ProdPlan plan = ProdPlan.builder()
                .item(item)
                .planQty(dto.getQuantity())
                .unit(dto.getUnit())
                .planDate(LocalDate.parse(dto.getPlan_date()))
                .dueDate(LocalDate.parse(dto.getDue_date()))
                .status(dto.getStatus())
                .manager(dto.getManager())
                .remark(dto.getNote())
                .build();

        prodPlanRepository.save(plan);
    }

    // ✅ 수정
    public void updatePlan(Long planId, ProdPlanRequestDTO dto) {
        ProdPlan plan = prodPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("계획 없음"));

        Item item = itemRepository.findById(dto.getItem_id())
                .orElseThrow(() -> new IllegalArgumentException("품목 없음"));

        plan.update(
                item,
                dto.getQuantity(),
                dto.getUnit(),
                LocalDate.parse(dto.getPlan_date()),
                LocalDate.parse(dto.getDue_date()),
                dto.getStatus(),
                dto.getManager(),
                dto.getNote()
        );
    }
}
