package com.team.berp.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team.berp.domain.CompanyOrder.OrderType;

public class OrderPageDto {
	private Long orderLineItemId;
	private Long orderId;
	private OrderType orderType;
	private Long orderQty;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate orderDate;
	private Long unitQty;
	private Long amount;
	private LocalDate dueDate;
	private String note;
	private Long companyId;
	private String companyName;
	private String companyEmpName;
	private Long employeeId;
	private String empName;
	private Long itemId;
	private String itemName;
	private String itemCode;
	private BigDecimal price;
	private String unit;
	
	
	// 전체 필드를 인자로 받는 생성자
    public OrderPageDto(Long orderLineItemId,
                    Long orderId,
                    String companyName,
                    String empName,
                    String itemCode,
                    String itemName,
                    Long orderQty,
                    String unit,
                    BigDecimal unitPrice,
                    LocalDate orderDate,
                    LocalDate dueDate,
                    Long unitQty,
                    Long amount) {
        this.orderLineItemId = orderLineItemId;
        this.orderId         = orderId;
        this.companyName     = companyName;
        this.empName         = empName;
        this.itemCode        = itemCode;
        this.itemName        = itemName;
        this.orderQty        = orderQty;
        this.unit            = unit;
        this.price       = unitPrice;
        this.orderDate       = orderDate;
        this.dueDate         = dueDate;
        this.unitQty         = unitQty;
        this.amount          = amount;
    }
}
