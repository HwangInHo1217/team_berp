package com.team.berp.bom.dto;

import java.util.List;

import lombok.Data;

@Data
public class UpdateBomRequestDTO {//현재 AddBomRequestDTO와 동일한 구조이지만 추후에 수정과 추가의 구조가 달라질 경우에 따라 분리하여 사용
    private Long parentItemId;
    private List<BomComponent> components;

    @Data
    public static class BomComponent {
        private Long childItemId; // 자재 item_id
        private int qty;         // 해당 자재의 수량
        private Integer seqNo;        // 순번
        private Double lossRt;        // 로스율
        private Integer itemPrice;    // 단가
        private String remark;        // 비고
    }
}

