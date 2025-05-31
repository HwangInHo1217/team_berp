// src/main/java/com/team/berp/order/dto/OrderSummaryDto.java
package com.team.berp.order.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 요약 정보 전용 DTO
 */
@Data
@NoArgsConstructor
public class OrderSummaryDto {
    private Long orderId;
    private String orderNum;
    private LocalDate orderDate;
    private String companyName;
    private String companyEmpName;
    private String empName;
    private Long orderQty;
    private Long amount;
    // (기타 필드 생략)

    // JPQL new 구문과 정확히 일치하는 생성자
    public OrderSummaryDto(
        Long orderId,
        String orderNum,
        LocalDate orderDate,
        String companyName,
        String empName,
        String companyEmpName,
        Long orderQty,
        Long amount
    ) {
        this.orderId   = orderId;
        this.orderNum  = orderNum;
        this.orderDate = orderDate;
        this.companyName = companyName;
        this.companyEmpName = companyEmpName;
        this.empName   = empName;
        this.orderQty  = orderQty;
        this.amount    = amount;
    }

}