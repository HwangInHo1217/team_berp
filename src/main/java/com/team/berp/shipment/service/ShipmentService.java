
package com.team.berp.shipment.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.InventoryLog;
import com.team.berp.domain.LogStatus;
import com.team.berp.domain.LogType;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.domain.Stock;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.order.repository.Order_CompanyOrderRepository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import com.team.berp.order.repository.Order_WarehouseRepository;
import com.team.berp.shipment.dto.ShipmentInfoDTO;
import com.team.berp.shipment.dto.ShipmentRequestDTO;
import com.team.berp.shipment.repository.ShipmentRepository;
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
			Long warehouseId = item.getWarehouseId();
			Integer qtyToShip = item.getQuantity();

			// 1-1) OrderLineItem 조회
			OrderLineItem orderLineItem = orderLineItemRepository.findById(orderLineItemId)
					.orElseThrow(() -> new IllegalArgumentException("해당 주문상품을 찾을 수 없습니다. ID=" + orderLineItemId));

			// 1-2) Stock 목록 조회 (itemId, warehouseId 기준)
			List<Stock> stocks = stockRepository.findByItemIdAndWarehouseId(orderLineItem.getItem().getId(),
					warehouseId);

			if (stocks.isEmpty()) {
				throw new IllegalArgumentException(
						"해당 품목의 재고가 존재하지 않습니다. 품목ID=" + orderLineItem.getItem().getId() + ", 창고ID=" + warehouseId);
			}

			// 1-3) 재고가 충분한 Stock을 찾거나, 단순히 첫 번째를 꺼내서 사용하는 방법
			// 여기서는 예시로 '재고가 qtyToShip 이상인 첫 번째 엔티티'를 찾아봅니다.
			Stock stock = stocks.stream().filter(s -> s.getQuantity() >= qtyToShip).findFirst()
					.orElseThrow(() -> new IllegalStateException(
							"재고가 부족합니다. 품목ID=" + orderLineItem.getItem().getId() + ", 창고ID=" + warehouseId));

			// 1-4) Stock 차감
			stock.setQuantity(stock.getQuantity() - qtyToShip);
			stockRepository.save(stock);

			// 1-5) InventoryLog(출고 로그) 생성 및 저장
			InventoryLog log = InventoryLog.builder().item(orderLineItem.getItem()).logDatetime(LocalDateTime.now())
					.quantity(qtyToShip).logType(LogType.OUT).logStatus(LogStatus.CONFIRMED).comment(comment)
					// .companyEmpName(companyEmpName) // 필요하다면 DTO에 필드를 추가하고 설정
					.warehouse(warehouseRepository.findById(warehouseId)
							.orElseThrow(() -> new IllegalArgumentException("창고를 찾을 수 없습니다. ID=" + warehouseId)))
					.orderLineItem(orderLineItem).build();

			shipmentRepository.save(log);
		}
	}

	/**
	 * 출고(OUT) 로그를 모두 조회해서 ShipmentInfoDTO 리스트로 반환
	 */
	
	  /**
     * 출고(OUT) + 창고이동(TRANSFER) 로그를 모두 조회해서
     * ShipmentInfoDTO 리스트로 반환.
     * 
     * - orderLineItem == null 인 경우 (TRANSFER 전용 로그)도 포함
     * - DTO 내부에서 널 체크하여, orderNum이나 companyName 등을 빈 문자열 처리
     */
    @Transactional(readOnly = true)
    public List<ShipmentInfoDTO> getAllShipments() {
        // 1) OUT 또는 TRANSFER 로그 모두를 최신 순으로 가져온다
        List<InventoryLog> logs = shipmentRepository
            .findByLogTypeInOrderByLogDatetimeDesc(
                Arrays.asList(LogType.OUT, LogType.TRANSFER)
            );

        // 2) 스트림으로 순회하며 널 체크를 한 뒤 DTO로 매핑
        return logs.stream()
            .map(log -> {
                // (A) 공통 필드: 반드시 존재하는 값들
                Long    logId       = log.getId();
                LocalDateTime logDt = log.getLogDatetime();
                LogType logType     = log.getLogType();
                Integer quantity    = log.getQuantity();
                String  warehouseName = (log.getWarehouse() != null)
                    ? log.getWarehouse().getWarehouseName() 
                    : "";

                // (B) item 정보 (item은 null이 아니라고 가정)
                String itemCode = "";
                String itemName = "";
                String itemUnit = "";
                if (log.getItem() != null) {
                    itemCode = log.getItem().getCode();
                    itemName = log.getItem().getName();
                    itemUnit = log.getItem().getUnit();
                }

                // (C) orderLineItem, companyOrder, companyName, empName, orderNum
                String companyName = "";
                String empName     = "";
                String orderNum    = "";

                OrderLineItem oli = log.getOrderLineItem();
                if (oli != null && oli.getCompanyOrder() != null) {
                    CompanyOrder co = oli.getCompanyOrder();

                    // (C1) CompanyName
                    if (co.getCompany() != null) {
                        companyName = co.getCompany().getCompanyName();
                        // (C2) 거래처 담당자(empName)
                        if (co.getCompany().getEmployee() != null) {
                            empName = co.getCompany().getEmployee().getEmpName();
                        }
                    }
                    // (C3) 주문번호
                    orderNum = (co.getOrderNum() != null) ? co.getOrderNum() : "";
                }

                // (D) comment
                String comment = (log.getComment() != null)
                    ? log.getComment()
                    : "";

                // 3) 최종 DTO 생성
                return new ShipmentInfoDTO(
                    logId,
                    logDt,
                    logType,        // OUT인지 TRANSFER인지 구분해서 전달할 수도 있습니다.
                    companyName,
                    itemCode,
                    itemName,
                    quantity,
                    itemUnit,
                    warehouseName,
                    empName,
                    comment,
                    orderNum
                );
            })
            .collect(Collectors.toList());
    }

	 // ✅ ② 새로 추가: logId 리스트를 받아서 InventoryLog 레코드를 삭제
    @Transactional
    public void deleteShipments(List<Long> logIds) {
        if (logIds == null || logIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 출고 로그 ID를 하나 이상 선택해주세요.");
        }
        // 1) 해당 logId들이 모두 존재하는지 간단히 체크 (선택사항)
        List<InventoryLog> logs = shipmentRepository.findAllById(logIds);
        if (logs.size() != logIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 출고 로그 ID가 포함되어 있습니다.");
        }
        // 2) 실제 삭제
        shipmentRepository.deleteAll(logs);
    }


}
