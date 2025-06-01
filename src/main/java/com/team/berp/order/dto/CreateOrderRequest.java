package com.team.berp.order.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class CreateOrderRequest {
    private Long companyId;
    private String orderType;
    private LocalDate orderDate;
    private String empName;
    private String companyEmpName;
    private String remark;
    private List<OrderItemRequest> items;
}
