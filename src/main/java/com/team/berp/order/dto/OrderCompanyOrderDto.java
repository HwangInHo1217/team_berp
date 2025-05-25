package com.team.berp.order.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team.berp.domain.CompanyOrder.OrderType;

import lombok.Data;

@Data
public class OrderCompanyOrderDto {
	private Integer order_id;
	private Integer company_id;
    private OrderType order_type;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime order_date;

}
