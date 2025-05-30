package com.team.berp.stock.controller;

import com.team.berp.inventory_log.dto.InventoryLogResponseDTO;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.stock.dto.*;
import com.team.berp.stock.service.StockService;
import com.team.berp.warehouse.service.Warehouse_service;
import com.team.berp.item.service.ItemService; // 기존 ItemService 사용
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     
     try {
         // 정렬 조건 파싱
         String[] sortParams = sort.split(",");
         Sort.Direction direction = sortParams.length > 1 && "ASC".equalsIgnoreCase(sortParams[1]) 
             ? Sort.Direction.ASC : Sort.Direction.DESC;
         
         Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
         
         // 실제 검색 로직 호출
         Page<StockResponseDTO> stocks = stockSvc.getList(keyword, whs, itemType, stockStatus, pageable);
         
         return ResponseEntity.ok(stocks);
         
     } catch (Exception e) {
         System.err.println("재고 목록 조회 오류: " + e.getMessage());
         return ResponseEntity.ok(Page.empty());
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
  * 🏢 창고 목록 - GET /api/stocks/warehouses
  */
 @GetMapping("/warehouses")
 public ResponseEntity<List<com.team.berp.warehouse.dto.WarehouseResponseDTO>> getWarehouses(
         @RequestParam(name = "useYn", defaultValue = "Y") String useYn) {
     try {
         return ResponseEntity.ok(whsSvc.getWhsByFilter(useYn));
     } catch (Exception e) {
         return ResponseEntity.ok(List.of());
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
  * 📊 엑셀 다운로드 - GET /api/stocks/excel (TODO)
  */
 @GetMapping("/excel")
 public ResponseEntity<byte[]> downloadExcel(
         @RequestParam(name = "keyword", required = false) String keyword,
         @RequestParam(name = "whs", required = false) String whs,
         @RequestParam(name = "itemType", required = false) String itemType,
         @RequestParam(name = "stockStatus", required = false) String stockStatus) {
     
     return ResponseEntity.ok(new byte[0]); // TODO: 엑셀 생성 로직
 }
}
