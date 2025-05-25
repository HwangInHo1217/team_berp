package com.team.berp.order.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team.berp.domain.CompanyOrder.OrderType;

import lombok.Data;

@Data
public class OrderAllListDto {
	private Integer order_line_item_id;
	private Integer order_qty;
	private Integer unit_price;
	private String item_code;
	private Integer order_id;
	private OrderType order_type;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime order_date;
    private Integer company_id;
	private String company_name;
	private Integer employee_id;
    private String emp_name;
	private Integer item_id;
	private String item_name;
	private String unit;
}
