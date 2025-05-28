package com.team.berp.plan.dto;

import com.team.berp.domain.ProdPlan.PlanStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdPlanRequestDTO {
	   private Long item_id;       // 선택된 품목 ID
	    private String plan_date;   // 계획일자
	    private Integer quantity;   // 계획수량
	    private String due_date;    // 납기일자
	    private String unit;        // 단위
	    private PlanStatus status;      // 상태 (계획/진행중/완료 등)
	    private String manager;     // 담당자
	    private String note;        // 비고
}
