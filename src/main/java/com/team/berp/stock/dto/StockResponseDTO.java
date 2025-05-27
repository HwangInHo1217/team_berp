package com.team.berp.stock.dto;

import com.team.berp.domain.Stock;
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
    
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String unit;
    
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    
    private Integer quantity;
    
    private String lotNumber;
    private LocalDateTime firstStockedDate;
    private LocalDateTime lastStockedDate;
    
    // 추가 필드
    private LocalDateTime lastInDate;    // 최종 입고일
    private LocalDateTime lastOutDate;   // 최종 출고일
    private Integer safetyStock;         // 안전재고
    private Boolean isBelowSafety;       // 안전재고 미달 여부
    private String stockStatus;          // 재고 상태 (정상, 부족, 없음)
    
    public static StockResponseDTO fromEntity(Stock stock) {
        Item item = stock.getItem();
        Warehouse warehouse = stock.getWarehouse();
        
        // 재고 상태 계산
        String stockStatus = "정상";
        if (stock.getQuantity() == 0) {
            stockStatus = "재고없음";
        } else if (stock.getQuantity() < 10) { // 임시 기준
            stockStatus = "재고부족";
        }
        
        return StockResponseDTO.builder()
                .stockId(stock.getId())
                .itemId(item.getId())
                .itemCode(item.getCode())
                .itemName(item.getName())
                .itemType(item.getType().name())
                .unit(item.getUnit())
                .warehouseId(warehouse.getId())
                .warehouseCode(warehouse.getWarehouseCode())
                .warehouseName(warehouse.getWarehouseName())
                .quantity(stock.getQuantity())
                .lotNumber(stock.getLotNumber())
                .firstStockedDate(stock.getFirstStockedDate())
                .lastStockedDate(stock.getLastStockedDate())
                .stockStatus(stockStatus)
                .isBelowSafety(false) // TODO: 안전재고 비교 로직 추가
                .build();
    }
    
    // 날짜 포맷팅 헬퍼 메소드
    public String getFormattedFirstStockedDate() {
        return firstStockedDate != null ? firstStockedDate.toLocalDate().toString() : "-";
    }
    
    public String getFormattedLastStockedDate() {
        return lastStockedDate != null ? lastStockedDate.toLocalDate().toString() : "-";
    }
    
    public String getFormattedLastInDate() {
        return lastInDate != null ? lastInDate.toLocalDate().toString() : "-";
    }
    
    public String getFormattedLastOutDate() {
        return lastOutDate != null ? lastOutDate.toLocalDate().toString() : "-";
    }
}