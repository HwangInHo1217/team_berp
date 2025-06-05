// File: /Team_BERP/src/main/java/com/team/berp/mrp/dto/MrpViewDto.java
package com.team.berp.mrp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MrpViewDto {
    private Long mrpId;          // MRP 번호
    private String itemCode;     // 품목코드
    private String itemName;     // 품목명
    private String itemType;     // 품목유형
    private String unit;         // 단위
    private String baseDate;     // 기준일자 (MRP 생성 일자)

    private int orderQty;        // 주문수량    ← “주문수량”을 새로 추가
    private int stockQty;        // 현재고
    private int shortageQty;     // 부족수량    ← “생산수량” 대신 부족수량만 표시

    private String source;       // 소요처
    private int leadTime;        // 리드타임(일)
    private String comment;      // 비고

    private String custName;     // 거래처명
    private String spec;         // 규격

    private String dueDate;      // 납기요청일
    private String mrpStatus;    // MRP 상태 (PLANNED, RELEASED, ...)
}
