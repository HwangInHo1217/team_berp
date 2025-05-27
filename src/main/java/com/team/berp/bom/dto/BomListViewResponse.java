package com.team.berp.bom.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BomListViewResponse { //특정 완제품의 BOM 구성 목록을 조회할 때 사용되는 응답 DTO.
    private String parentCode;
    private String parentName;
    private List<Component> components;

    @Data
    @AllArgsConstructor
    public static class Component {
        private String childCode;
        private String childName;
        private int qty;
        private String spec;  // 규격
        private String unit;  // 단위

        private Integer seqNo;       // 순번
        private String lossRate;     // 예: "5%"
        private String unitPrice;    // 문자열로 포맷된 금액 또는 단위
        private String remark;
    }
}
