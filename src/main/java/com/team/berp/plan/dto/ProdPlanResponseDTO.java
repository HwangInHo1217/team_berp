package com.team.berp.plan.dto;

import java.time.LocalDate;

import com.team.berp.domain.ProdPlan;
import com.team.berp.domain.ProdPlan.PlanStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ProdPlanResponseDTO {
    private Long planId;
    private String itemCode;     // 품목코드
    private String itemName;     // 품목명
    private Integer planQty;
    private String unit;
    private LocalDate planDate;
    private LocalDate dueDate;
    private LocalDate startDate;
    private PlanStatus status;
    private Integer priority;
    private String manager;
    private String remark;
 // ✅ 엔티티로부터 DTO를 만드는 생성자 추가
    public ProdPlanResponseDTO(ProdPlan plan) {
        this.planId = plan.getPlanId();
        this.itemCode = plan.getItem().getCode();
        this.itemName = plan.getItem().getName();
        this.planQty = plan.getPlanQty();
        this.unit = plan.getUnit();
        this.planDate = plan.getPlanDate();
        this.dueDate = plan.getDueDate();
        this.startDate = plan.getStartDate();
        this.status = plan.getStatus();
        this.priority = plan.getPriority();
        this.manager = plan.getManager();
        this.remark = plan.getRemark();
    }

}
