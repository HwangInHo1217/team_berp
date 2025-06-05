package com.team.berp.bom.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BomTreeDTO {
    private Long itemId;
    private String itemName;
    private List<BomTreeDTO> children; // 자식 노드를 재귀적으로 담음
}
