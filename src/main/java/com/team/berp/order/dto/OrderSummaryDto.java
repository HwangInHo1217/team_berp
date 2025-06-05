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
//src/main/java/com/team/berp/order/dto/OrderSummaryDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {
 private Long      orderId;
 private String    orderNum;
 private LocalDate orderDate;
 private String    companyName;
 private String    companyEmpName;
 private String    empName;
 private Integer      orderQty;
 private Long      amount;
//↓ 추가
 private Boolean allShipped;
 /**
 * 기존 JPQL 생성자(new OrderSummaryDto(8개 인자))가 호출하던 시그니처를
 * 그대로 남겨 놓습니다. allShipped 은 기본값(false)으로 설정해 둡니다.
 */
public OrderSummaryDto(
        Long      orderId,
        String    orderNum,
        LocalDate orderDate,
        String    companyName,
        String    companyEmpName,
        String    empName,
        Integer   orderQty,
        Long      amount
) {
    this.orderId        = orderId;
    this.orderNum       = orderNum;
    this.orderDate      = orderDate;
    this.companyName    = companyName;
    this.companyEmpName = companyEmpName;
    this.empName        = empName;
    this.orderQty       = orderQty;
    this.amount         = amount;
    this.allShipped     = false;  // created‐by‐JPQL 때는 기본값(false)
}
}
