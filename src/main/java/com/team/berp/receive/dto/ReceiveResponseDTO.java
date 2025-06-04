package com.team.berp.receive.dto;

import com.team.berp.domain.InventoryLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 입고 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveResponseDTO {
    
    private Long logId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String warehouseName;
    private Integer quantity;
    private String unit;
    private LocalDateTime receiveDate;
    private String receiveType; // ORDER_BASED, INDEPENDENT
    private String status;
    private String note;
    
    // 발주 관련 정보
    private String orderNumber;
    private String companyName;
    private String managerName;
    
    /**
     * InventoryLog 엔티티로부터 DTO 생성
     */
    public static ReceiveResponseDTO fromInventoryLog(InventoryLog log) {
        ReceiveResponseDTOBuilder builder = ReceiveResponseDTO.builder()
                .logId(log.getId())
                .itemCode(log.getItem().getCode())
                .itemName(log.getItem().getName())
                .itemType(log.getItem().getType().name())
                .warehouseName(log.getWarehouse().getWarehouseName())
                .quantity(log.getQuantity())
                .unit(log.getItem().getUnit())
                .receiveDate(log.getLogDatetime())
                .status(log.getLogStatus() != null ? log.getLogStatus().name() : "CONFIRMED")
                .note(log.getComment());
        
        // 발주 관련 정보 설정
        if (log.getOrderLineItem() != null) {
            builder.receiveType("ORDER_BASED")
                   .orderNumber(log.getOrderLineItem().getCompanyOrder().getOrderNum())
                   .companyName(log.getOrderLineItem().getCompanyOrder().getCompany().getCompanyName());
            
            if (log.getOrderLineItem().getCompanyOrder().getCompany().getEmployee() != null) {
                builder.managerName(log.getOrderLineItem().getCompanyOrder().getCompany().getEmployee().getEmpName());
            }
        } else {
            builder.receiveType("INDEPENDENT");
        }
        
        return builder.build();
    }
    
    /**
     * 날짜 포맷팅
     */
    public String getFormattedReceiveDate() {
        return receiveDate != null ? receiveDate.toLocalDate().toString() : "-";
    }
    
    /**
     * 입고 유형 라벨
     */
    public String getReceiveTypeLabel() {
        return switch (receiveType) {
            case "ORDER_BASED" -> "발주 기반";
            case "INDEPENDENT" -> "독립적 입고";
            default -> receiveType;
        };
    }
    
    /**
     * 상태 배지 클래스
     */
    public String getStatusBadgeClass() {
        return switch (status) {
            case "CONFIRMED" -> "bg-success";
            case "PENDING" -> "bg-warning text-dark";
            default -> "bg-secondary";
        };
    }
}