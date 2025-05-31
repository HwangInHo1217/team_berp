package com.team.berp.bom.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class UpdateBomRequestDTO {
    private Long versionId;
    private Long parentItemId;
    private String description;
    private String useYn;

    private List<BomComponent> components;

    @Data
    public static class BomComponent {
        private Long childItemId;
        private Integer seqNo;
        private Integer qty;
        private BigDecimal lossRt;
        private Integer itemPrice;
        private String remark;
    }
}


