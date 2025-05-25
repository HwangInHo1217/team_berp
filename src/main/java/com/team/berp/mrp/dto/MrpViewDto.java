package com.team.berp.mrp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: getter/setter/toString 등 자동 생성
@AllArgsConstructor // 모든 필드 값을 받는 생성자 자동 생성
@NoArgsConstructor  // 기본 생성자(파라미터 없는 생성자) 자동 생성
public class MrpViewDto {
    private String itemCode;     // 품목코드 (item 테이블에 추가된 경우)
    private String itemName;     // 품목명
    private String itemType;     // 품목유형 (raw/semi/product)
    private String unit;         // 단위(kg, EA 등)
    private String baseDate;     // 기준일자 (ex: 생산계획 날짜 등)
    private int requiredQty;     // 필요수량 (MRP 결과)
    private int stockQty;        // 현재고 (재고 테이블)
    private int confirmedQty;    // 확정수량 (생산/발주 등)
    private int shortageQty;     // 부족수량 (계산값)
    private String source;       // 소요처 (생산계획명 등)
    private int leadTime;        // 리드타임(소요기간, 일)
    private String comment;      // 비고(메모 등)
}
