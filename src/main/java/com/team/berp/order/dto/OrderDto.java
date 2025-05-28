package com.example.order.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 목록 및 상세 조회용 DTO
 */
public class OrderDto {
    private String orderNum;
    private String companyName;
    private String empName;
    private String companyEmpName;
    private LocalDate orderDate;
    private int orderQty;
    private int amount;
    private String remark;
    private List<OrderLineItemDto> items;

    /**
     * 목록 조회용 생성자
     */
    public OrderDto(String orderNum, String companyName, String empName,
                    String companyEmpName, LocalDate orderDate,
                    long orderQty, long amount) {
        this.orderNum = orderNum;
        this.companyName = companyName;
        this.empName = empName;
        this.companyEmpName = companyEmpName;
        this.orderDate = orderDate;
        this.orderQty = (int) orderQty;
        this.amount = (int) amount;
    }

    /**
     * 상세 조회용 생성자
     */
    public OrderDto(String orderNum, String companyName, String empName,
                    String companyEmpName, LocalDate orderDate,
                    String remark, List<OrderLineItemDto> items) {
        this(orderNum, companyName, empName, companyEmpName,
             orderDate, items.size(), items.stream()
                                          .mapToInt(OrderLineItemDto::getUnitPriceAll)
                                          .sum());
        this.remark = remark;
        this.items = items;
    }

    // Getters and setters omitted for brevity
}