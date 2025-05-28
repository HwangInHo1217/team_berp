package com.team.berp.plan.dto;

import java.time.LocalDate;

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
}
