
package com.team.berp.shipment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.domain.*;

import com.team.berp.item.repository.ItemRepository;
import com.team.berp.order.repository.Order_CompanyOrderRepository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import com.team.berp.order.repository.Order_WarehouseRepository;
import com.team.berp.shipment.dto.ShipmentInfoDTO;
import com.team.berp.shipment.dto.ShipmentRequestDTO;

import com.team.berp.shipment.repository.ShipmentRepository;
import com.team.berp.shipment.repository.Shipment_InventoryLogRepository;
import com.team.berp.shipment.repository.Shipment_StockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipmentService {

	private final Order_CompanyOrderRepository order_CompanyOrderRepository;
	private final ShipmentRepository shipmentRepository;
	private final ItemRepository itemRepository;
	private final Order_WarehouseRepository warehouseRepository;
	private final Order_OrderLineItemRepository orderLineItemRepository;
	private final Shipment_InventoryLogRepository inventoryLogRepository;
	private final Shipment_StockRepository stockRepository;

	/*
	 * 출고 등록 (재고 차감 + InventoryLog 기록)
	 */
	@Transactional
	public void createShipment(ShipmentRequestDTO request) {
	    Long orderId = request.getOrderId();
	    String comment = request.getComment();
	    String companyEmpName = request.getCompanyEmpName();

	    // 1) shipmentItems 순회
	    for (ShipmentRequestDTO.ShipmentItem item : request.getShipmentItems()) {
	        Long orderLineItemId = item.getOrderLineItemId();
	        Long warehouseId      = item.getWarehouseId();
	        Integer qtyToShip     = item.getQuantity();

	        // 1-1) OrderLineItem 조회
	        OrderLineItem orderLineItem = orderLineItemRepository.findById(orderLineItemId)
	                .orElseThrow(() -> new IllegalArgumentException("해당 주문상품을 찾을 수 없습니다. ID=" + orderLineItemId));

	        // 1-2) Stock 목록 조회 (itemId, warehouseId 기준)
	        List<Stock> stocks = stockRepository.findByItemIdAndWarehouseId(
	                orderLineItem.getItem().getId(), warehouseId);

	        if (stocks.isEmpty()) {
	            throw new IllegalArgumentException("해당 품목의 재고가 존재하지 않습니다. 품목ID=" +
	                    orderLineItem.getItem().getId() + ", 창고ID=" + warehouseId);
	        }

	        // 1-3) 재고가 충분한 Stock을 찾거나, 단순히 첫 번째를 꺼내서 사용하는 방법
	        //    여기서는 예시로 '재고가 qtyToShip 이상인 첫 번째 엔티티'를 찾아봅니다.
	        Stock stock = stocks.stream()
	                .filter(s -> s.getQuantity() >= qtyToShip)
	                .findFirst()
	                .orElseThrow(() -> new IllegalStateException(
	                        "재고가 부족합니다. 품목ID=" + orderLineItem.getItem().getId() + ", 창고ID=" + warehouseId));

	        // 1-4) Stock 차감
	        stock.setQuantity(stock.getQuantity() - qtyToShip);
	        stockRepository.save(stock);

	        // 1-5) InventoryLog(출고 로그) 생성 및 저장
	        InventoryLog log = InventoryLog.builder()
	                .item(orderLineItem.getItem())
	                .logDatetime(LocalDateTime.now())
	                .quantity(qtyToShip)
	                .logType(LogType.OUT)
	                .logStatus(LogStatus.CONFIRMED)
	                .comment(comment)
	                // .companyEmpName(companyEmpName) // 필요하다면 DTO에 필드를 추가하고 설정
	                .warehouse(
	                    warehouseRepository.findById(warehouseId)
	                        .orElseThrow(() -> new IllegalArgumentException("창고를 찾을 수 없습니다. ID=" + warehouseId))
	                )
	                .orderLineItem(orderLineItem)
	                .build();

	        inventoryLogRepository.save(log);
	    }
	}


	/**
	 * 출고(OUT) 로그를 모두 조회해서 ShipmentInfoDTO 리스트로 반환
	 */
	@Transactional(readOnly = true)
	public List<ShipmentInfoDTO> getAllShipments() {
		List<InventoryLog> logs = shipmentRepository.findByLogTypeOrderByLogDatetimeDesc(LogType.OUT);

		// InventoryLog → ShipmentInfoDTO 로 매핑
		return logs.stream().map(log -> {
			// InventoryLog에서 연결된 엔티티들을 안전하게 가져오기
			CompanyOrder companyOrder = log.getOrderLineItem().getCompanyOrder();
			String companyName = (companyOrder.getCompany() != null) ? companyOrder.getCompany().getCompanyName() : "";

			return new ShipmentInfoDTO(log.getId(), log.getLogDatetime(), companyName, log.getItem().getCode(),
					log.getItem().getName(), log.getQuantity(), log.getItem().getUnit(),
					log.getWarehouse().getWarehouseName(),
					log.getOrderLineItem().getCompanyOrder().getEmployee() != null
							? log.getOrderLineItem().getCompanyOrder().getEmployee().getEmpName()
							: "",
					log.getComment());
		}).collect(Collectors.toList());
	}

}