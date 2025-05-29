
package com.team.berp.shipment.service;

import com.team.berp.shipment.dto.*;
import com.team.berp.domain.*;
import com.team.berp.shipment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {@Override
	public List<ShipmentDetailDto> getAllShipments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<OrderSummaryDto> getPendingOrders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderDetailDto getOrderDetail(Long orderId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ShipmentDetailDto getShipmentDetail(Long shipmentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void preShipOrders(List<Long> orderIds) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shipOrders(List<Long> orderIds) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateShipment(Long shipmentId, ShipmentDetailDto form) {
		// TODO Auto-generated method stub
		
	}}