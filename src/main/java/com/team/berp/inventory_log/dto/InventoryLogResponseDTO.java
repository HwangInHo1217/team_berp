// InventoryLogResponseDTO.java - 재고 이력 응답
package com.team.berp.inventory_log.dto;

import com.team.berp.domain.InventoryLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class InventoryLogResponseDTO {
    
    private Long logId;
    private String logType;
    private String logTypeLabel;
    private String itemCode;
    private String itemName;
    private String warehouseName;
    private Integer quantity;
    private LocalDateTime logDatetime;
    private String comment;
    private String logStatus;
    
    public static InventoryLogResponseDTO fromEntity(InventoryLog log) {
        return InventoryLogResponseDTO.builder()
                .logId(log.getId())
                .logType(log.getLogType().name())
                .logTypeLabel(log.getLogType().getLabel())
                .itemCode(log.getItem().getCode())
                .itemName(log.getItem().getName())
                .warehouseName(log.getWarehouse().getWarehouseName())
                .quantity(log.getQuantity())
                .logDatetime(log.getLogDatetime())
                .comment(log.getComment())
                .logStatus(log.getLogStatus() != null ? log.getLogStatus().name() : null)
                .build();
    }
    
    public String getFormattedLogDatetime() {
        return logDatetime != null ? 
            logDatetime.toLocalDate().toString() + " " + 
            logDatetime.toLocalTime().toString().substring(0, 5) : "-";
    }
}