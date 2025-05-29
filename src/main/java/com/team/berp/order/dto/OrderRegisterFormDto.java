// File: src/main/java/com/team/berp/order/dto/OrderRegisterFormDto.java
package com.team.berp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 등록/수정 폼 바인딩 DTO
 * - 고객사 선택 → 자동 empName, companyEmpName 세팅
 * - orderType 고정: CUSTOMER
 * - dueDate는 미노출
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRegisterFormDto {
    private String companyName;      // 선택된 고객사명
    private LocalDate orderDate;     // 필수
    private String note;             // 선택
    private List<OrderLineItemDto> items;  // 최소 1개 이상
    private String orderType = "CUSTOMER";
}