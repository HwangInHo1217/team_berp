package com.team.berp.stock.dto;

import com.team.berp.domain.Item;
import com.team.berp.domain.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class StockResponseDTO {

    private Long stockId;

    private String itemCode;
    private String itemName;

    private String warehouseCode;
    private String warehouseName;

    private Integer quantity;

    private String lotNum;          // 로트 번호
    private LocalDateTime firstAt;  // 최초 입고일
    private LocalDateTime lastAt;   // 마지막 재고 변경일

    public static StockResponseDTO fromEntity(com.team.berp.domain.Stock stock) {
        Item item = stock.getItem();
        Warehouse whs = stock.getWhs();

        return StockResponseDTO.builder()
                .stockId(stock.getId())
                .itemCode(item.getCode())
                .itemName(item.getName())
                .warehouseCode(whs.getWarehouseCode())
                .warehouseName(whs.getWarehouseName())
                .quantity(stock.getQuantity())
                .lotNum(stock.getLotNum())
                .firstAt(stock.getStockedAt())
                .lastAt(stock.getUpdatedAt())
                .build();
    }
}
