// File: src/main/java/com/team/berp/order/dto/OrderPageDto.java
package com.team.berp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 페이징된 주문 목록 응답 DTO */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPageDto {
    private List<OrderDto> content;  // 주문 리스트
    private int page;                // 0-based 현재 페이지
    private int size;                // 요청한 페이지 크기
    private long totalElements;      // 전체 주문 수
    private int totalPages;          // 전체 페이지 수
}