// StockRequestDTO.java - 개선된 버전
package com.team.berp.stock.dto;

import lombok.Data;

@Data
public class StockRequestDTO {
    
    private Long itemId;
    private Long warehouseId;
    private Integer quantity;
    private String lotNumber;
    private String comment;  // 입출고 사유
}