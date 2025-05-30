// File: /Team_BERP/src/main/java/com/team/berp/bom/dto/BomListViewResponse.java
package com.team.berp.bom.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BomListViewResponse {
    private String parentCode;
    private String parentName;
    private List<Component> components;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Component {
        private String childCode;       // 자재코드
        private String childName;       // 자재명
        private int    qty;             // 소요량
        private String spec;            // 규격
        private String unit;            // 단위

        private Integer seqNo;          // 순번
        private String  lossRate;       // 예: "5%"
        private String  unitPrice;      // 단위 포맷된 금액
        private String  remark;         // 비고

        // ───────── NEW ─────────
        private int    stockQty;        // 현재고
        private int    shortageQty;     // 부족수량
        private int    safetyStock;     // 안전재고
        private int    purchaseQty;     // 발주필요수량
        private int    purchaseLeadTime;// 구매 리드타임
        private String expectedDate;    // 예상입고일
        // ───────────────────────
    }
}
