package com.team.berp.order.dto;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.team.berp.domain.CompanyOrder.OrderType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderFormDto {
	private Integer order_line_item_id;
	
	private Integer order_id;
	
	@NotNull
    private OrderType order_type;
	
	@NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime order_date;
	
	private Integer company_id;
	
	@NotNull
	@Size(max = 100)
    private String company_name;
	
	private Integer employee_id;
	
	@NotBlank
    @Size(max = 50)
    private String emp_name;
	
	private Integer item_id;
	
	@NotBlank
    @Size(max = 100)
    private String item_name;
	
	@NotNull @Min(1)
    private Integer order_qty;
}
