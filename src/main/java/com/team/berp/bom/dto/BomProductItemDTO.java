package com.team.berp.bom.dto;

import com.team.berp.domain.ItemType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//bom.html에 테이블 형식으로 list를 뿌리기 위한 dto
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BomProductItemDTO {
    private Long id;
    private String code;
    private String name;
    private String spec;
    private String unit;
    private String use;
    private ItemType type;

}
