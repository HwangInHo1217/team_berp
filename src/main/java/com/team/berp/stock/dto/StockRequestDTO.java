package com.team.berp.stock.dto;

import lombok.Data;

@Data
public class StockRequestDTO {

    private Long itemId;
    private Integer warehouseId;

    private Integer quantity;

    private String lotNum; // 로트번호 (선택 입력)

    // 📌 입고일자 등은 DB에서 자동처리하므로 프론트에서 입력받지 않음
}
