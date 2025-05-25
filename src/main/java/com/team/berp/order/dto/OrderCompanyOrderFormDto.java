package com.team.berp.order.dto;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.team.berp.domain.CompanyOrder.OrderType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCompanyOrderFormDto {
	private Integer order_id;
	private Integer company_id;
	
	@NotNull
    private OrderType order_type;
	
	@NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime order_date;

}
