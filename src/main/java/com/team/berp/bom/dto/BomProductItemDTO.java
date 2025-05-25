package com.team.berp.bom.dto;

import com.team.berp.domain.ItemType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class BomProductItemDTO {
    private Integer id;
    private String code;
    private String name;
    private String spec;
    private String unit;
    private String use;
    private ItemType type;
}
