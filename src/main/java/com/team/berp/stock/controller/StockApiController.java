package com.team.berp.stock.controller;

import com.team.berp.inventory_log.dto.InventoryLogResponseDTO;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.stock.dto.*;
import com.team.berp.stock.service.StockService;
import com.team.berp.warehouse.service.Warehouse_service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockApiController {
    
    private final StockService stockSvc;
    private final InventoryLogService logSvc;
    private final Warehouse_service whsSvc;
    
    // 재고 목록 조회
    @GetMapping
    public ResponseEntity<Page<StockResponseDTO>> getStocks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String whs,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,DESC") String sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort.split(",")));
        Page<StockResponseDTO> stocks = stockSvc.getList(keyword, whs, itemType, stockStatus, pageable);
        
        return ResponseEntity.ok(stocks);
    }
    
    // 재고 상세 조회
    @GetMapping("/{stockId}")
    public ResponseEntity<StockResponseDTO> getStockDetail(@PathVariable Long stockId) {
        return ResponseEntity.ok(stockSvc.getDetail(stockId));
    }
    
    // 입고 처리
    @PostMapping("/in")
    public ResponseEntity<Map<String, String>> stockIn(@RequestBody StockRequestDTO req) {
        stockSvc.stockIn(req);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "입고 처리가 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    // 출고 처리
    @PostMapping("/out")
    public ResponseEntity<Map<String, String>> stockOut(@RequestBody StockRequestDTO req) {
        stockSvc.stockOut(req);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "출고 처리가 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    // 개별 재고 출고 (stockId 기반)
    @PostMapping("/{stockId}/out")
    public ResponseEntity<Map<String, String>> stockOutById(
            @PathVariable Long stockId,
            @RequestBody Map<String, Integer> request) {
        
        StockResponseDTO stock = stockSvc.getDetail(stockId);
        StockRequestDTO req = new StockRequestDTO();
        req.setItemId(stock.getItemId());
        req.setWarehouseId(stock.getWarehouseId());
        req.setQuantity(request.get("quantity"));
        req.setComment(request.get("comment") != null ? String.valueOf(request.get("comment")) : null);
        
        stockSvc.stockOut(req);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "출고 처리가 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    // 폐기 처리
    @PostMapping("/dispose")
    public ResponseEntity<Map<String, String>> dispose(@RequestBody StockRequestDTO req) {
        stockSvc.dispose(req);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "폐기 처리가 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    // 반품입고 처리
    @PostMapping("/return-in")
    public ResponseEntity<Map<String, String>> returnIn(@RequestBody StockRequestDTO req) {
        stockSvc.returnIn(req);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "반품입고 처리가 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    // 재고 조정 (실사)
    @PostMapping("/{stockId}/adjust")
    public ResponseEntity<Map<String, String>> adjustStock(
            @PathVariable Long stockId,
            @RequestBody StockAdjustDTO req) {
        
        stockSvc.adjustStock(stockId, req.getActualQuantity(), req.getReason());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "재고 조정이 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    // 재고 이력 조회
    @GetMapping("/{stockId}/history")
    public ResponseEntity<Page<InventoryLogResponseDTO>> getStockHistory(
            @PathVariable Long stockId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        StockResponseDTO stock = stockSvc.getDetail(stockId);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<InventoryLogResponseDTO> history = logSvc.getStockHistory(
            stock.getItemId(), stock.getWarehouseId(), pageable);
        
        return ResponseEntity.ok(history);
    }
    
    // 창고 목록 조회 (동적 로딩용)
    @GetMapping("/warehouses")
    public ResponseEntity<?> getWarehouses(@RequestParam(defaultValue = "Y") String useYn) {
        return ResponseEntity.ok(whsSvc.getActiveWarehouses());
    }
    
    // 엑셀 다운로드
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String whs,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String stockStatus) {
        
        // TODO: Apache POI를 사용한 엑셀 생성 로직
        // byte[] excelData = stockSvc.generateExcel(keyword, whs, itemType, stockStatus);
        // return ResponseEntity.ok()
        //         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock_list.xlsx")
        //         .contentType(MediaType.APPLICATION_OCTET_STREAM)
        //         .body(excelData);
        
        return ResponseEntity.ok(new byte[0]); // 임시
    }
}