package com.team.berp.order.dto;
import java.time.LocalDate;
import lombok.Data;
@Data
public class OrderDto {
    private Long orderId;
    private String orderNum;
    private LocalDate orderDate;
    private String companyName;
    private int orderLineCount;
    private Long amount;
    private String empName;
    private String companyEmpName;
}