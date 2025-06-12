package com.team.berp.shipment.dto;

import java.time.LocalDateTime;

import com.team.berp.domain.LogType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShipmentInfoDTO {
    private Long              logId;
    private LocalDateTime     logDatetime;
    private LogType           logType;         // OUT / TRANSFER
    private String            companyName;     // 주문과 연결된 회사명(없으면 "")
    private String            itemCode;
    private String            itemName;
    private Integer           quantity;
    private String            unit;
    private String            warehouseName;
    private String            empName;         // 거래처 담당자(없으면 "")
    private String            comment;         // 비고
    private String            orderNum;        // 주문번호 (없으면 "")
}
