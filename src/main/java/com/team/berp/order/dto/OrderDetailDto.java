package com.team.berp.order.dto;
import java.util.List;
import lombok.Data;
@Data
public class OrderDetailDto {
    private Long orderId;
    private String orderNum;
    private List<OrderLineItemDto> items;
}