package com.team.berp.order.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class OrderPageDto {
    private List<OrderDto> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    // getters/setters 생략
}