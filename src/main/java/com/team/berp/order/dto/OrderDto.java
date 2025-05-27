package com.team.berp.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team.berp.domain.CompanyOrder.OrderType;

import lombok.Data;

@Data
public class OrderDto {
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
	private BigDecimal unitPrice;
	private String unit;
}
