package com.team.berp.shipment.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 출고 등록 모달에서 특정 주문의 전체 상세를 담는 DTO
 */
@Data
@NoArgsConstructor
public class OrderDetailDto {
    private Long orderId;
    private LocalDate orderDate;
    private String companyName;
    private String empName;
    private String companyEmpName;
    private List<OrderItemDetailDto> items;  // 주문에 속한 모든 품목
}