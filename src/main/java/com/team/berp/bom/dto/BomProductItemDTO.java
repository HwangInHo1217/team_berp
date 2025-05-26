package com.team.berp.bom.dto;

import com.team.berp.domain.ItemType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data

public class BomProductItemDTO {
    private Long id;
    private String code;
    private String name;
    private String spec;
    private String unit;
    private String use;
    private ItemType type;
    
    
    public BomProductItemDTO() {
    }

    public BomProductItemDTO(Long id, String code, String name, String spec, String unit, String use, ItemType type) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.spec = spec;
        this.unit = unit;
        this.use = use;
        this.type = type;
    }
}
