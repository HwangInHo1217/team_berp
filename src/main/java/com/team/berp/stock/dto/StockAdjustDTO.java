// StockAdjustDTO.java - 재고 조정용
package com.team.berp.stock.dto;

import lombok.Data;

@Data
public class StockAdjustDTO {
    private Integer actualQuantity;
    private String reason;
}
