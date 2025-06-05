package com.team.berp.shipment.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentRequestDTO {
	 private Long orderId;                     // 출고를 생성할 CompanyOrder ID
	    private String comment;                   // 출고 코멘트(선택)
	    private String companyEmpName;            // 주문 담당자(선택)

	    private List<ShipmentItem> shipmentItems; // 실제 출고할 각 품목/창고/수량 목록


	    @Data
	    @AllArgsConstructor
	    @NoArgsConstructor
	    @Builder
	    public static class ShipmentItem {
	        private Long orderLineItemId;          // 어느 OrderLineItem에서 출고할 건지
	        private Long warehouseId;              // 어느 창고에서 출고할 건지
	        private Integer quantity;              // 출고수량
	    }
}
