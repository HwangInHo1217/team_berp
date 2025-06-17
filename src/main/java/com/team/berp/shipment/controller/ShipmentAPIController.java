// /team_berp/src/main/java/com/team/berp/shipment/controller/ShipmentAPIController.java
package com.team.berp.shipment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.shipment.dto.ShipmentInfoDTO;
import com.team.berp.shipment.dto.ShipmentRequestDTO;
import com.team.berp.shipment.service.ShipmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ShipmentAPIController {

	private final ShipmentService shipmentService;

	  /**
     * 출고 정보 등록 (POST /api/shipments)
     * 
     * 요청바디(JSON) 예시:
     * {
     *   "orderId": 123,
     *   "comment": "긴급 출고 요청",
     *   "companyEmpName": "홍길동",
     *   "shipmentItems": [
     *     { "orderLineItemId": 456, "warehouseId": 11, "quantity": 2 },
     *     { "orderLineItemId": 456, "warehouseId": 12, "quantity": 3 },
     *     { "orderLineItemId": 789, "warehouseId": 15, "quantity": 1 }
     *   ]
     * }
     */
    @PostMapping("/api/shipments")
    public ResponseEntity<Void> createShipment(@RequestBody ShipmentRequestDTO request) {
        shipmentService.createShipment(request);
        return ResponseEntity.ok().build();
    }

	/**
	 * 출고 목록 조회 (GET /api/shipments) Response Body 예시 (JSON 목록): [ { "logId": 12,
	 * "logDatetime": "2025-06-01T14:32:05", "companyName": "삼성전자", "itemCode":
	 * "P001", "itemName": "완제품 A", "quantity": 200, "unit": "EA", "warehouseName":
	 * "완제품 창고", "companyEmpName": "김민호", "comment": "긴급 출고" }, ... ]
	 */
	@GetMapping("/api/shipments")
	public ResponseEntity<List<ShipmentInfoDTO>> getShipments() {
		List<ShipmentInfoDTO> list = shipmentService.getAllShipments();
		return ResponseEntity.ok(list);
	}
	
	   // ───────────────────────────────────────────────────────
    // ✅ ① 체크박스로 넘어온 “logId 리스트”를 삭제해주는 엔드포인트 추가
    @DeleteMapping("/api/shipments-del")
    public ResponseEntity<String> deleteShipments(@RequestBody List<Long> logIds) {
        try {
            shipmentService.deleteShipments(logIds);
            return ResponseEntity.ok("삭제 완료");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body("삭제 실패: " + e.getMessage());
        }
    }
    
    
    @PostMapping("api/shipment/transfer")
    public ResponseEntity<?> transfer(@RequestBody ShipmentInfoDTO dto){
    	shipmentService.saveTransfer(dto);
    	
    	
    	return ResponseEntity.ok().body("등록 완료");
    }
}
