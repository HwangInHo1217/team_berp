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
    }
}
