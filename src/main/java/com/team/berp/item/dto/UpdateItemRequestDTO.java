// File: com/team/berp/item/dto/UpdateItemRequestDTO.java
package com.team.berp.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateItemRequestDTO {
    private String name;
    private String type;
    private String spec;
    private String unit;
    private String use;

    // ↓ 이 세 줄을 추가하세요 ↓
    private Long   itemPrice;
    private Integer safetyStock;
    private Integer purchaseLeadTime;
}
