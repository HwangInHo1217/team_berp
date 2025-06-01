package com.team.berp.stock.dto;

import lombok.Data;

/**
 * 창고간 재고 이동 요청 DTO
 * 
 * 프론트엔드에서 창고 이동 요청시 전달받는 데이터
 * - 출발 재고 정보
 * - 도착 창고 정보  
 * - 이동 수량 및 사유
 */
@Data
public class StockTransferRequestDTO {
    
    /** 출발 재고 ID (Stock 테이블의 PK) */
    private Long fromStockId;
    
    /** 출발 창고 ID */
    private Long fromWarehouseId;
    
    /** 도착 창고 ID */  
    private Long toWarehouseId;
    
    /** 품목 ID */
    private Long itemId;
    
    /** 이동 수량 */
    private Integer quantity;
    
    /** 이동 사유 코드 */
    private String reason;
    
    /** 상세 설명 */
    private String comment;
}