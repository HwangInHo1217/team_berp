package com.team.berp.order.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class OrderFormListDto {
	private OrderOrderLineItemFormDto orderLineItem;
	private OrderCompanyOrderFormDto companyOrder;
	private OrderCompanyFormDto company;
	private OrderEmployeeFormDto employee;
	private OrderItemFormDto item;
	private List<OrderFormDto> orderFormList = new ArrayList<>();
}
