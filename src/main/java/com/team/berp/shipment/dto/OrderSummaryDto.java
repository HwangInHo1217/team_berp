package com.team.berp.shipment.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 출고 대기(Pre-ship) 주문 요약 정보 DTO
 */
@Data
@AllArgsConstructor
public class OrderSummaryDto {
    private Long orderId;                // CompanyOrder.orderId
    private LocalDate orderDate;         // 주문일자
    private String companyName;          // 고객사명
    private Long orderQty;               // 품목 수
    private Long amount;                 // 총 금액
    private String empName;              // 담당자 (Company.employee.empName)
    private String companyEmpName;       // 거래처 담당자 (Company.companyEmpName)
}