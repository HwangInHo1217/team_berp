package com.team.berp.shipment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
	private boolean success;
	private String message;
	private boolean allShipped;
	
	public ShipmentResponse(Boolean allShipped) {
		this.allShipped = allShipped;
	}
}
