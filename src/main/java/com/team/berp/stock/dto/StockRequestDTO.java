package com.team.berp.stock.dto;

import lombok.Data;

/**
 * 재고 요청 DTO
 * - 입고, 출고, 폐기 등의 재고 변동 요청에 사용
 * - LOT 번호 관련 필드 제거
 * - 필수 필드만 포함하여 단순화
 */
@Data
public class StockRequestDTO {
    
    /** 품목 ID (필수) */
    private Long itemId;
    
    /** 창고 ID (필수) */
    private Long warehouseId;
    
    /** 변동 수량 (필수) */
    private Integer quantity;
    
    /** 변동 사유 또는 코멘트 (선택) */
    private String comment;
    
    // LOT 번호 필드 제거
    // private String lotNumber; // 제거됨
    
    /**
     * 유효성 검증 메서드
     */
    public boolean isValid() {
        return itemId != null && 
               warehouseId != null && 
               quantity != null && 
               quantity > 0;
    }
    
    /**
     * 디버깅용 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("StockRequestDTO{itemId=%d, warehouseId=%d, quantity=%d, comment='%s'}", 
                           itemId, warehouseId, quantity, comment);
    }
}