package com.team.berp.receive.dto;

import lombok.Data;

/**
 * 입고 요청 DTO - 발주기반/독립적 입고 구분
 */
@Data
public class ReceiveRequestDTO {
    
    /** 품목 ID (필수) */
    private Long itemId;
    
    /** 창고 ID (필수) */
    private Long warehouseId;
    
    /** 입고 수량 (필수) */
    private Integer quantity;
    
    /** 입고 유형 (필수) - ORDER_BASED: 발주기반, INDEPENDENT: 독립적 입고 */
    private String receiveType;
    
    /** 발주 라인 아이템 ID (발주기반 입고시 필수, 독립적 입고시 null) */
    private Long orderLineItemId;
    
    /** 공급업체 ID (선택) */
    private Long supplierId;
    
    /** 비고 (선택) */
    private String note;
    
    /**
     * 유효성 검증
     */
    public boolean isValid() {
        return itemId != null && 
               warehouseId != null && 
               quantity != null && 
               quantity > 0 &&
               receiveType != null;
    }
    
    /**
     * 입고 코멘트 생성
     */
    public String generateComment() {
        StringBuilder comment = new StringBuilder();
        
        if ("ORDER_BASED".equals(receiveType)) {
            comment.append("[발주기반 입고]");
        } else {
            comment.append("[독립적 입고]");
        }
        
        if (note != null && !note.trim().isEmpty()) {
            comment.append(" ").append(note);
        }
        
        return comment.toString();
    }
}
