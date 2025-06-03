package com.team.berp.stock.dto;

import lombok.Data;

/**
 * 긴급출고 요청 DTO
 * - 거래처 선택 기능 포함
 * - 긴급출고 시 필요한 모든 정보 포함
 */
@Data
public class QuickOutRequestDTO {
    
    /** 품목 ID (필수) */
    private Long itemId;
    
    /** 창고 ID (필수) */
    private Long warehouseId;
    
    /** 출고 수량 (필수, 최대 10개) */
    private Integer quantity;
    
    /** 거래처 ID (선택 - 긴급출고 시 어느 고객에게 보내는지) */
    private Long companyId;
    
    /** 출고 사유 (선택) */
    private String reason;
    
    /** 추가 코멘트 (선택) */
    private String comment;
    
    /**
     * 유효성 검증 메서드
     */
    public boolean isValid() {
        return itemId != null && 
               warehouseId != null && 
               quantity != null && 
               quantity > 0 && 
               quantity <= 10; // 긴급출고는 최대 10개 제한
    }
    
    /**
     * 디버깅용 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("QuickOutRequestDTO{itemId=%d, warehouseId=%d, quantity=%d, companyId=%s, reason='%s', comment='%s'}", 
                           itemId, warehouseId, quantity, companyId, reason, comment);
    }
}