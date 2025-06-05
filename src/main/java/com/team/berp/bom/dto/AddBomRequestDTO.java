package com.team.berp.bom.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class AddBomRequestDTO {

    // 🔹 추가: 버전 코드 (예: V1, V-PORK)
    //private String versionCode;
    private String description;
    private String useYn;
    // 완제품의 ID (item 테이블의 item_id)
    private Long parentItemId;

    // 자재 구성 목록 (자재 ID + 수량 등)
    private List<BomComponent> components;

    // 내부 static 클래스: 각 자재 구성 1건을 의미
    @Data
    public static class BomComponent {
        private Long childItemId;     // 자재 item_id
        private int qty;              // 해당 자재의 수량
        private Integer seqNo;        // 순번
        private BigDecimal lossRt;        // 로스율
        private Integer itemPrice;    // 단가
        private String remark;        // 비고
    }
}
