package com.team.berp.stock.controller;

import com.team.berp.inventory_log.dto.InventoryLogResponseDTO;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.stock.dto.*;
import com.team.berp.stock.service.StockService;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.service.Warehouse_service;
import com.team.berp.item.service.ItemService; // 기존 ItemService 사용
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 재고 관리 API 컨트롤러
 * - 재고 조회, 입고, 출고, 재고조정 등의 HTTP 요청을 처리
 * - 프론트엔드의 fetch() 요청을 받아서 적절한 Service로 위임
 */
//=============================================================================
//📦 StockApiController.java - 재고 API 컨트롤러
//=============================================================================

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockApiController {
 
 private final StockService stockSvc;
 private final InventoryLogService logSvc;
 private final Warehouse_service whsSvc;
 private final ItemService itemSvc;
 
 /**
  * 📋 재고 목록 조회 - GET /api/stocks
  * 파라미터 → 페이징 설정 → Service 호출 → 결과 반환
  */
 @GetMapping
 public ResponseEntity<Page<StockResponseDTO>> getStocks(
         @RequestParam(name = "keyword", required = false) String keyword,
         @RequestParam(name = "whs", required = false) String whs,
         @RequestParam(name = "itemType", required = false) String itemType,
         @RequestParam(name = "stockStatus", required = false) String stockStatus,
         @RequestParam(name = "page", defaultValue = "0") int page,
         @RequestParam(name = "size", defaultValue = "10") int size,
         @RequestParam(name = "sort", defaultValue = "id,DESC") String sort) {
	 System.out.println("🔍 받은 sort 파라미터: " + sort);
     System.out.println("📄 page: " + page + ", size: " + size);
     System.out.println("🔎 keyword: " + keyword);
     System.out.println("🏢 warehouse: " + whs);
     System.out.println("📦 itemType: " + itemType);
     System.out.println("📊 stockStatus: " + stockStatus);
	 
	 try {
         // 정렬 조건 파싱
         String[] sortParams = sort.split(",");
         Sort.Direction direction = sortParams.length > 1 && "ASC".equalsIgnoreCase(sortParams[1]) 
             ? Sort.Direction.ASC : Sort.Direction.DESC;
         
         Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
         
         // 실제 검색 로직 호출
         Page<StockResponseDTO> stocks = stockSvc.getList(keyword, whs, itemType, stockStatus, pageable);
         System.out.println("✅ 조회 결과 건수: " + stocks.getTotalElements());
         System.out.println("📋 실제 반환 데이터 수: " + stocks.getContent().size());
         
         // 처음 3개 데이터의 창고명 출력
         if (!stocks.getContent().isEmpty()) {
             System.out.println("🏢 처음 3개 창고명:");
             stocks.getContent().stream()
                 .limit(3)
                 .forEach(stock -> System.out.println("  - " + stock.getWarehouseName()));
         }
         System.out.println("====================================================");
         return ResponseEntity.ok(stocks);
         
     } catch (Exception e) {
    	 System.out.println("❌ 조회 중 에러: " + e.getClass().getSimpleName() + " - " + e.getMessage());
    	 e.printStackTrace();
    	 //return ResponseEntity.ok(Page.empty());
    	 System.out.println("====================================================");
    	 throw e;
     }
 }
 
 /**
  * 🔍 재고 상세 조회 - GET /api/stocks/{stockId}
  */
 @GetMapping("/{stockId}")
 public ResponseEntity<StockResponseDTO> getStockDetail(@PathVariable("stockId") Long stockId) {
     try {
         return ResponseEntity.ok(stockSvc.getDetail(stockId));
     } catch (Exception e) {
         return ResponseEntity.badRequest().build();
     }
 }
 
 /**
  * 🖥️ 모달용 상세 조회 - GET /api/stocks/{stockId}/detail
  */
 @GetMapping("/{stockId}/detail")
 public ResponseEntity<StockResponseDTO> getStockDetailForModal(@PathVariable("stockId") Long stockId) {
     try {
         return ResponseEntity.ok(stockSvc.getDetail(stockId));
     } catch (Exception e) {
         return ResponseEntity.badRequest().build();
     }
 }
 
 /**
  * 📜 재고 로그 조회 - GET /api/stocks/{stockId}/logs
  * stockId → 재고정보 → 품목+창고 → 이력조회
  */
 @GetMapping("/{stockId}/logs")
 public ResponseEntity<List<InventoryLogResponseDTO>> getStockLogs(@PathVariable("stockId") Long stockId) {
     try {
         StockResponseDTO stock = stockSvc.getDetail(stockId);
         Pageable pageable = PageRequest.of(0, 100);
         
         Page<InventoryLogResponseDTO> logs = logSvc.getStockHistory(
             stock.getItemId(), stock.getWarehouseId(), pageable);
         
         return ResponseEntity.ok(logs.getContent());
     } catch (Exception e) {
         return ResponseEntity.ok(List.of());
     }
 }
 
 /**
  * 📦 재고 입고 - POST /api/stocks/in
  * 기존재고 있으면 수량증가, 없으면 신규생성 + 이력기록
  */
 @PostMapping("/in")
 public ResponseEntity<Map<String, String>> stockIn(@RequestBody StockRequestDTO req) {
     try {
         stockSvc.stockIn(req);
         
         Map<String, String> response = new HashMap<>();
         response.put("status", "success");
         response.put("message", "입고 처리가 완료되었습니다.");
         return ResponseEntity.ok(response);
     } catch (Exception e) {
         Map<String, String> response = new HashMap<>();
         response.put("status", "error");
         response.put("message", "입고 처리 실패: " + e.getMessage());
         return ResponseEntity.badRequest().body(response);
     }
 }
 
 /**
  * 📤 재고 출고 - POST /api/stocks/out
  * 재고수량 확인 → 차감 → 이력기록
  */
 @PostMapping("/out")
 public ResponseEntity<Map<String, String>> stockOut(@RequestBody StockRequestDTO req) {
     try {
         stockSvc.stockOut(req);
         
         Map<String, String> response = new HashMap<>();
         response.put("status", "success");
         response.put("message", "출고 처리가 완료되었습니다.");
         return ResponseEntity.ok(response);
     } catch (Exception e) {
         Map<String, String> response = new HashMap<>();
         response.put("status", "error");
         response.put("message", "출고 처리 실패: " + e.getMessage());
         return ResponseEntity.badRequest().body(response);
     }
 }
 
 /**
  * ⚡ 빠른 출고 - POST /api/stocks/{stockId}/out
  * stockId로 품목+창고 자동조회 → 출고처리
  */
 @PostMapping("/{stockId}/out")
 public ResponseEntity<Map<String, String>> stockOutById(
         @PathVariable("stockId") Long stockId,
         @RequestBody Map<String, Object> request) {
     
     try {
         // stockId → 재고정보 조회
         StockResponseDTO stock = stockSvc.getDetail(stockId);
         
         // 출고 요청 객체 생성
         StockRequestDTO req = new StockRequestDTO();
         req.setItemId(stock.getItemId());
         req.setWarehouseId(stock.getWarehouseId());
         req.setQuantity((Integer) request.get("quantity"));
         req.setComment(request.get("comment") != null ? 
             String.valueOf(request.get("comment")) : "빠른출고");
         
         stockSvc.stockOut(req);
         
         Map<String, String> response = new HashMap<>();
         response.put("status", "success");
         response.put("message", "출고 처리가 완료되었습니다.");
         return ResponseEntity.ok(response);
     } catch (Exception e) {
         Map<String, String> response = new HashMap<>();
         response.put("status", "error");
         response.put("message", "출고 처리 실패: " + e.getMessage());
         return ResponseEntity.badRequest().body(response);
     }
 }
 
 /**
  * 📊 페이징 이력 조회 - GET /api/stocks/{stockId}/history
  */
 @GetMapping("/{stockId}/history")
 public ResponseEntity<Page<InventoryLogResponseDTO>> getStockHistory(
         @PathVariable("stockId") Long stockId,
         @RequestParam(name = "page", defaultValue = "0") int page,
         @RequestParam(name = "size", defaultValue = "10") int size) {
     
     try {
         StockResponseDTO stock = stockSvc.getDetail(stockId);
         Pageable pageable = PageRequest.of(page, size);
         
         Page<InventoryLogResponseDTO> history = logSvc.getStockHistory(
             stock.getItemId(), stock.getWarehouseId(), pageable);
         
         return ResponseEntity.ok(history);
     } catch (Exception e) {
         return ResponseEntity.ok(Page.empty());
     }
 }
 

 
 /**
  * 📦 품목 목록 - GET /api/stocks/items
  * Entity → Map 변환으로 필요 필드만 선택
  */
 @GetMapping("/items")
 public ResponseEntity<List<Map<String, Object>>> getItems(
         @RequestParam(name = "useYn", defaultValue = "Y") String useYn) {
     
     try {
         List<com.team.berp.domain.Item> allItems = itemSvc.getItemList(
             org.springframework.data.domain.Pageable.unpaged()
         ).getContent();
         
         // Entity → Map 변환
         List<Map<String, Object>> result = allItems.stream()
             .filter(item -> useYn.equals("ALL") || useYn.equals(item.getUse()))
             .map(item -> {
                 Map<String, Object> itemMap = new HashMap<>();
                 itemMap.put("id", item.getId());
                 itemMap.put("code", item.getCode());
                 itemMap.put("name", item.getName());
                 itemMap.put("type", item.getType().name());
                 itemMap.put("unit", item.getUnit());
                 itemMap.put("use", item.getUse());
                 return itemMap;
             })
             .collect(java.util.stream.Collectors.toList());
             
         return ResponseEntity.ok(result);
     } catch (Exception e) {
         return ResponseEntity.ok(List.of());
     }
 }
 
 /**
  * 📊 엑셀 다운로드 - GET /api/stocks/excel
  */
 @GetMapping("/excel")
 public ResponseEntity<byte[]> downloadExcel(
         @RequestParam(name = "keyword", required = false) String keyword,
         @RequestParam(name = "whs", required = false) String whs,
         @RequestParam(name = "itemType", required = false) String itemType,
         @RequestParam(name = "stockStatus", required = false) String stockStatus) {
     
	    try {
	        // 1. JPA로 전체 데이터 조회 (기존 Service 그대로 활용)
	        Pageable allData = PageRequest.of(0, 10000); // 충분히 큰 사이즈
	        Page<StockResponseDTO> stocks = stockSvc.getList(keyword, whs, itemType, stockStatus, allData);
	        
	        // 2. 🔥 UTF-8 BOM 추가해서 한글 깨짐 해결
	        StringBuilder csv = new StringBuilder();
	        csv.append("\uFEFF"); // UTF-8 BOM - 엑셀에서 한글 제대로 인식
	        csv.append("순번,품목코드,품목명,품목유형,창고명,현재수량,단위,최종입고일,최종출고일\n");
	        
	        // 3. 🔥 순번 자동 생성 (1부터 시작)
	        int seq = 1;
	        for (StockResponseDTO stock : stocks.getContent()) {
	            csv.append(seq++).append(",") // 순번 자동 증가
	               .append(stock.getItemCode() != null ? stock.getItemCode() : "").append(",")
	               .append(stock.getItemName() != null ? stock.getItemName() : "").append(",")
	               .append(stock.getItemType() != null ? stock.getItemType() : "").append(",")
	               .append(stock.getWarehouseName() != null ? stock.getWarehouseName() : "").append(",")
	               .append(stock.getQuantity() != null ? stock.getQuantity() : 0).append(",")
	               .append(stock.getUnit() != null ? stock.getUnit() : "").append(",")
	               .append(stock.getLastInDate() != null ? stock.getLastInDate() : "").append(",")
	               .append(stock.getLastOutDate() != null ? stock.getLastOutDate() : "").append("\n");
	        }
	        
	        // 4. 🔥 응답 헤더 수정 - 한글 파일명 지원
	        HttpHeaders headers = new HttpHeaders();
	        headers.add("Content-Type", "application/vnd.ms-excel; charset=UTF-8");
	        
	        // 한글 파일명 인코딩
	        String fileName = "재고현황_" + java.time.LocalDate.now() + ".csv";
	        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
	                                                   .replaceAll("\\+", "%20");
	        headers.add("Content-Disposition", 
	                   "attachment; filename*=UTF-8''" + encodedFileName);
	        
	        return ResponseEntity.ok()
	                .headers(headers)
	                .body(csv.toString().getBytes("UTF-8"));
	                
	    } catch (Exception e) {
	        e.printStackTrace();
			return ResponseEntity.status(500).body("오류 발생".getBytes());
		}
	}
 	
 

/**
 * 🔄 창고간 재고 이동 - POST /api/stocks/transfer
 */
@PostMapping("/transfer")
public ResponseEntity<Map<String, String>> transferStock(@RequestBody StockTransferRequestDTO req) {
    
    System.out.println("🔄 창고이동 API 요청: " + req);
    
    try {
        stockSvc.transferStock(req);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "창고 이동이 완료되었습니다.");
        
        System.out.println("✅ 창고이동 성공");
        return ResponseEntity.ok(response);
        
    } catch (IllegalArgumentException e) {
        System.err.println("❌ 창고이동 유효성 오류: " + e.getMessage());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
        
    } catch (Exception e) {
        System.err.println("❌ 창고이동 시스템 오류: " + e.getMessage());
        e.printStackTrace();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "창고 이동 중 오류가 발생했습니다: " + e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}

/**
 * 📊 재고 현황 요약 - GET /api/stocks/summary
 */
@GetMapping("/summary")
public ResponseEntity<Map<String, Object>> getStockSummary() {
    try {
        Map<String, Object> summary = stockSvc.getStockSummary();
        return ResponseEntity.ok(summary);
        
    } catch (Exception e) {
        System.err.println("재고 요약 조회 실패: " + e.getMessage());
        
        // 실패해도 빈 데이터 반환 (선택적 기능이므로)
        Map<String, Object> emptySummary = new HashMap<>();
        emptySummary.put("totalItems", 0);
        emptySummary.put("belowSafety", 0);
        emptySummary.put("outOfStock", 0);
        emptySummary.put("normalStock", 0);
        
        return ResponseEntity.ok(emptySummary);
    }
}

/**
 * 🏢 창고 목록 조회 (기존 메서드 개선)
 * 기존의 /api/stocks/warehouses 를 warehouse 서비스와 연동하도록 수정
 */
@GetMapping("/warehouses")
public ResponseEntity<List<Map<String, Object>>> getWarehouses(
        @RequestParam(name = "useYn", defaultValue = "Y") String useYn) {
    try {
        
        // 🔥 기존 창고 서비스 활용!
        List<WarehouseResponseDTO> warehouses = whsSvc.getWhsByFilter(useYn);
        
        // Warehouse DTO → Map 변환 (JavaScript 호환성)
        List<Map<String, Object>> result = warehouses.stream()
            .map(wh -> {
                Map<String, Object> whMap = new HashMap<>();
                whMap.put("id", wh.getWarehouseId());  // ✅ 정확한 필드명
                whMap.put("warehouseCode", wh.getWarehouseCode());
                whMap.put("warehouseName", wh.getWarehouseName());
                whMap.put("warehouseType", wh.getWarehouseType().name());
                whMap.put("useYn", wh.getUseYn());
                return whMap;
            })
            .collect(Collectors.toList());
            
        System.out.println("🏢 창고 목록 조회 성공: " + result.size() + "개");
        return ResponseEntity.ok(result);
        
    } catch (Exception e) {
        System.err.println("창고 목록 조회 실패: " + e.getMessage());
        return ResponseEntity.ok(List.of()); // 빈 리스트 반환
    }
}
 
 
 
 
 
 
}
