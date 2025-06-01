package com.team.berp.stock.dto;

import com.team.berp.domain.Stock;
import com.team.berp.domain.Item;
import com.team.berp.domain.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 재고 응답 DTO
 * - LOT 번호 관련 필드 제거
 * - JavaScript 호환성을 위한 필드 추가
 * - 재고 상태 계산 로직 포함
 */
@Data
@Builder
@AllArgsConstructor
public class StockResponseDTO {
    
    private Long stockId;
    
    // 품목 정보
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String unit;
    private String itemUnit;  // JavaScript에서 사용하는 필드명
    
    // 창고 정보
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    
    // 재고 정보
    private Integer quantity;
    
    // 시간 정보
    private LocalDateTime firstStockedDate;
    private LocalDateTime lastStockedDate;
    
    // JavaScript에서 사용하는 추가 필드들
    private LocalDateTime firstAt;           // 대체 필드
    private LocalDateTime lastAt;            // 대체 필드
    private LocalDateTime lastInDate;        // 최종 입고일
    private LocalDateTime lastOutDate;       // 최종 출고일
    private LocalDateTime actualLastInAt;    // JS에서 사용
    private LocalDateTime actualLastOutAt;   // JS에서 사용
    
    // 재고 상태 정보
    private Integer safetyStock;         // 안전재고
    private Boolean isBelowSafety;       // 안전재고 미달 여부
    private String stockStatus;          // 재고 상태 (정상, 부족, 없음)
    
    // 기본 생성자 추가
    public StockResponseDTO() {}
    
    /**
     * Entity에서 DTO로 변환하는 정적 팩토리 메서드
     * LOT 번호 관련 로직 제거
     */
    public static StockResponseDTO fromEntity(Stock stock) {
        Item item = stock.getItem();
        Warehouse warehouse = stock.getWarehouse();
        
        // 재고 상태 계산
        String stockStatus = calculateStockStatus(stock.getQuantity());
        
        return StockResponseDTO.builder()
                .stockId(stock.getId())
                .itemId(item.getId())
                .itemCode(item.getCode())
                .itemName(item.getName())
                .itemType(item.getType().name())
                .unit(item.getUnit())
                .itemUnit(item.getUnit()) // JS용 동일 값
                .warehouseId(warehouse.getId())
                .warehouseCode(warehouse.getWarehouseCode())
                .warehouseName(warehouse.getWarehouseName())
                .quantity(stock.getQuantity())
                .firstStockedDate(stock.getFirstStockedDate())
                .lastStockedDate(stock.getLastStockedDate())
                .firstAt(stock.getFirstStockedDate()) // JS용 대체
                .lastAt(stock.getLastStockedDate())   // JS용 대체
                .stockStatus(stockStatus)
                .isBelowSafety(calculateBelowSafety(stock.getQuantity()))
                .safetyStock(item.getSafetyStock()) // 품목의 안전재고 정보
                .build();
    }
    
    /**
     * 재고 상태 계산
     */
    private static String calculateStockStatus(Integer quantity) {
        if (quantity == null || quantity == 0) return "재고없음";
        if (quantity < 10) return "재고부족"; // 임시 기준 (향후 품목별 안전재고 기준으로 변경)
        return "정상";
    }
    
    /**
     * 안전재고 미달 여부 계산
     */
    private static Boolean calculateBelowSafety(Integer quantity) {
        return quantity != null && quantity < 10; // 임시 안전재고 기준
    }
    
    // === 날짜 포맷팅 헬퍼 메소드들 ===
    
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
    
    // === 재고 상태 배지용 메서드 ===
    
    /**
     * 프론트엔드 배지 표시용 상태 코드
     */
    public String getStatusBadgeClass() {
        return switch (stockStatus) {
            case "재고없음" -> "bg-danger";
            case "재고부족" -> "bg-warning";
            default -> "bg-success";
        };
    }
    
    /**
     * 재고 수량 포맷팅 (천단위 구분)
     */
    public String getFormattedQuantity() {
        if (quantity == null) return "0";
        return String.format("%,d", quantity);
    }
}