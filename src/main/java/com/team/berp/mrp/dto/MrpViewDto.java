package com.team.berp.mrp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MrpViewDto {
    private String itemCode;     // 품목코드 (item 테이블에 추가된 경우)
    private String itemName;     // 품목명
    private String itemType;     // 품목유형
    private String unit;         // 단위
    private String baseDate;     // 기준일자 (prod_plan 등에서)
    private int requiredQty;     // 필요수량 (mrp)
    private int stockQty;        // 현재고 (stock)
    private int confirmedQty;    // 확정수량 (생산/발주 등)
    private int shortageQty;     // 부족수량 (계산)
    private String source;       // 소요처 (생산계획명 등)
    private int leadTime;        // 리드타임(일)
    private String comment;      // 비고
}
