package com.example.order.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 등록/수정 폼 데이터 바인딩용 DTO
 */
public class OrderRegisterFormDto {
    private String orderType = "CUSTOMER"; // 항상 CUSTOMER
    private String companyName;
    private LocalDate orderDate;
    private String remark;
    private List<OrderLineItemDto> items;

    // 기본 생성자, getters/setters 생략
}