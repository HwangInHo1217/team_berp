package com.team.berp.order.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * 등록 폼 DTO (dueDate 제거)
 */
@Data
public class OrderRegisterFormDto {
    private Long customerId;
    private LocalDate orderDate;
    private String manager;
    private String note;
    private List<OrderLineItemDto> items;

    public OrderRegisterFormDto() {}

    // getters / setters …
}
