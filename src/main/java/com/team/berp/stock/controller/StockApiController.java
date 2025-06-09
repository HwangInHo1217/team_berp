package com.team.berp.stock.controller;

import com.team.berp.inventory_log.dto.InventoryLogResponseDTO;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.stock.dto.*;
import com.team.berp.stock.service.StockBusinessService;
import com.team.berp.stock.service.StockService;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.service.Warehouse_service;
import com.team.berp.item.service.ItemService;
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
import java.util.stream.Collectors;

/**
 * 재고 관리 API 컨트롤러 (개선된 버전)
 * - LOT 번호 관련 기능 제거
 * - 백엔드 중심의 비즈니스 로직 강화
 * - 창고간 이동 및 품목 유형별 검증 포함
 * - 에러 처리 및 로깅 개선
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockApiController {
 
    private final StockService stockSvc;
    private final InventoryLogService logSvc;
    private final Warehouse_service whsSvc;
    private final ItemService itemSvc;
    private final StockBusinessService stockBiz;
    /**
     * 재고 목록 조회 - GET /api/stocks
     * 복합 필터링 및 정렬 지원
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
        
        System.out.println("🔍 재고 조회 API 요청:");
        System.out.println("  - keyword: " + keyword);
        System.out.println("  - warehouse: " + whs);
        System.out.println("  - itemType: " + itemType);
        System.out.println("  - stockStatus: " + stockStatus);
        System.out.println("  - page: " + page + ", size: " + size);
        System.out.println("  - sort: " + sort);
	 
	    try {
            // 정렬 조건 파싱 및 검증
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && "ASC".equalsIgnoreCase(sortParams[1]) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            
            // 정렬 필드 유효성 검증
            sortField = validateAndMapSortField(sortField);
            
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
            
            // 실제 검색 로직 호출
            Page<StockResponseDTO> stocks = stockSvc.getList(keyword, whs, itemType, stockStatus, pageable);
            
            System.out.println("✅ 조회 완료 - 총 " + stocks.getTotalElements() + "건");
            System.out.println("📋 현재 페이지 데이터: " + stocks.getContent().size() + "건");
            
            return ResponseEntity.ok(stocks);
            
        } catch (Exception e) {
            System.err.println("❌ 재고 조회 API 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("재고 조회 중 오류가 발생했습니다.", e);
        }
    }
 
    /**
     * 재고 상세 조회 - GET /api/stocks/{stockId}
     */
    @GetMapping("/{stockId}")
    public ResponseEntity<StockResponseDTO> getStockDetail(@PathVariable("stockId") Long stockId) {
        try {
            System.out.println("🔍 재고 상세 조회 - stockId: " + stockId);
            StockResponseDTO stock = stockSvc.getDetail(stockId);
            System.out.println("✅ 상세 조회 완료: " + stock.getItemName());
            return ResponseEntity.ok(stock);
        } catch (RuntimeException e) {
            System.err.println("❌ 재고 상세 조회 실패: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("❌ 재고 상세 조회 오류: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
 
    /**
     * 모달용 상세 조회 - GET /api/stocks/{stockId}/detail
     */
    @GetMapping("/{stockId}/detail")
    public ResponseEntity<StockResponseDTO> getStockDetailForModal(@PathVariable("stockId") Long stockId) {
        return getStockDetail(stockId); // 동일한 로직 재사용
    }
 
    /**
     * 재고 이력 조회 (페이징) - GET /api/stocks/{stockId}/history
     */
    @GetMapping("/{stockId}/history")
    public ResponseEntity<Page<InventoryLogResponseDTO>> getStockHistory(
            @PathVariable("stockId") Long stockId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        
        try {
            System.out.println("📜 재고 이력 조회 - stockId: " + stockId);
            
            StockResponseDTO stock = stockSvc.getDetail(stockId);
            Pageable pageable = PageRequest.of(page, size);
            
            Page<InventoryLogResponseDTO> history = logSvc.getStockHistory(
                stock.getItemId(), stock.getWarehouseId(), pageable);
            
            System.out.println("✅ 이력 조회 완료 - " + history.getTotalElements() + "건");
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            System.err.println("❌ 재고 이력 조회 오류: " + e.getMessage());
            return ResponseEntity.ok(Page.empty());
        }
    }
 
    /**
     * 재고 입고 - POST /api/stocks/in
     * LOT 번호 필드 제거
     */
    @PostMapping("/in")
    public ResponseEntity<Map<String, String>> stockIn(@RequestBody StockRequestDTO req) {
        try {
            System.out.println("📦 재고 입고 요청: " + req);
            
            // 유효성 검증
            if (!req.isValid()) {
                throw new IllegalArgumentException("필수 입력값이 누락되었습니다.");
            }
            
            stockSvc.stockIn(req);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "입고 처리가 완료되었습니다.");
            
            System.out.println("✅ 입고 처리 완료");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 입고 요청 유효성 오류: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 입고 처리 시스템 오류: " + e.getMessage());
            return createErrorResponse("입고 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
 
    /**
     * 재고 출고 - POST /api/stocks/out
     */
    @PostMapping("/out")
    public ResponseEntity<Map<String, String>> stockOut(@RequestBody StockRequestDTO req) {
        try {
            System.out.println("📤 재고 출고 요청: " + req);
            
            if (!req.isValid()) {
                throw new IllegalArgumentException("필수 입력값이 누락되었습니다.");
            }
            
            stockSvc.stockOut(req);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "출고 처리가 완료되었습니다.");
            
            System.out.println("✅ 출고 처리 완료");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 출고 요청 유효성 오류: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("❌ 출고 처리 비즈니스 오류: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 출고 처리 시스템 오류: " + e.getMessage());
            return createErrorResponse("출고 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
 
    /**
     * 빠른 출고 - POST /api/stocks/{stockId}/out
     * stockId로 품목+창고 자동조회 → 출고처리
     */
    @PostMapping("/{stockId}/out")
    public ResponseEntity<Map<String, String>> stockOutById(
            @PathVariable("stockId") Long stockId,
            @RequestBody Map<String, Object> request) {
        
        try {
            System.out.println("⚡ 빠른 출고 요청 - stockId: " + stockId);
            
            // stockId → 재고정보 조회
            StockResponseDTO stock = stockSvc.getDetail(stockId);
            
            // 출고 수량 검증
            Integer quantity = (Integer) request.get("quantity");
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("출고 수량이 올바르지 않습니다.");
            }
            
            if (quantity > stock.getQuantity()) {
                throw new IllegalArgumentException("출고 수량이 현재고보다 많습니다.");
            }
            
            // 출고 요청 객체 생성
            StockRequestDTO req = new StockRequestDTO();
            req.setItemId(stock.getItemId());
            req.setWarehouseId(stock.getWarehouseId());
            req.setQuantity(quantity);
            req.setComment(request.get("comment") != null ? 
                String.valueOf(request.get("comment")) : "빠른출고");
            
            stockSvc.stockOut(req);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "빠른 출고가 완료되었습니다.");
            
            System.out.println("✅ 빠른 출고 완료");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 빠른 출고 유효성 오류: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 빠른 출고 시스템 오류: " + e.getMessage());
            return createErrorResponse("빠른 출고 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 창고간 재고 이동 - POST /api/stocks/transfer
     * 품목 유형별 창고 제한 및 유효성 검증 포함
     */
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, String>> transferStock(@RequestBody StockTransferRequestDTO req) {
        
        System.out.println("🔄 창고이동 API 요청: " + req);
        
        try {
            // 기본 유효성 검증
            validateTransferRequest(req);
            
            stockSvc.transferStock(req);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "창고 이동이 완료되었습니다.");
            
            System.out.println("✅ 창고이동 성공");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 창고이동 유효성 오류: " + e.getMessage());
            return createErrorResponse(e.getMessage());
            
        } catch (RuntimeException e) {
            System.err.println("❌ 창고이동 비즈니스 오류: " + e.getMessage());
            return createErrorResponse(e.getMessage());
            
        } catch (Exception e) {
            System.err.println("❌ 창고이동 시스템 오류: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("창고 이동 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 재고 현황 요약 - GET /api/stocks/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getStockSummary() {
        try {
            System.out.println("📊 재고 요약 통계 요청");
            Map<String, Object> summary = stockSvc.getStockSummary();
            System.out.println("✅ 재고 요약 조회 완료: " + summary);
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            System.err.println("❌ 재고 요약 조회 실패: " + e.getMessage());
            
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
     * 창고 목록 조회 - GET /api/stocks/warehouses
     * 🔧 미사용 창고 필터링 개선
     */
    @GetMapping("/warehouses")
    public ResponseEntity<List<Map<String, Object>>> getWarehouses(
            @RequestParam(name = "useYn", defaultValue = "Y") String useYn) {
        try {
            System.out.println("🏢 창고 목록 조회 요청 - useYn: " + useYn);
            
            // 🔧 기존 창고 서비스 활용 + 사용 중인 창고만 필터링
            List<WarehouseResponseDTO> warehouses;
            
            if ("ALL".equals(useYn)) {
                // 전체 조회 시에도 기본적으로는 사용 중인 창고만 (특별한 경우가 아니면)
                warehouses = whsSvc.getWhsByFilter("Y");
                System.out.println("⚠️ 재고 관리에서는 사용 중인 창고만 표시 (useYn=ALL이지만 Y로 필터링)");
            } else {
                warehouses = whsSvc.getWhsByFilter(useYn);
            }
            
            // Warehouse DTO → Map 변환 (JavaScript 호환성)
            List<Map<String, Object>> result = warehouses.stream()
                .filter(wh -> "Y".equals(wh.getUseYn())) // 🔧 추가 안전장치: 사용 중인 창고만
                .map(wh -> {
                    Map<String, Object> whMap = new HashMap<>();
                    whMap.put("id", wh.getWarehouseId());
                    whMap.put("warehouseCode", wh.getWarehouseCode());
                    whMap.put("warehouseName", wh.getWarehouseName());
                    whMap.put("warehouseType", wh.getWarehouseType().name());
                    whMap.put("useYn", wh.getUseYn());
                    
                    // 🆕 미사용 창고 표시용 추가 정보
                    whMap.put("isActive", "Y".equals(wh.getUseYn()));
                    whMap.put("displayName", "Y".equals(wh.getUseYn()) ? 
                        wh.getWarehouseName() : 
                        wh.getWarehouseName() + " (미사용)");
                    
                    return whMap;
                })
                .collect(Collectors.toList());
                
            System.out.println("✅ 창고 목록 조회 성공: " + result.size() + "개 (사용 중인 창고만)");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ 창고 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.ok(List.of()); // 빈 리스트 반환
        }
    }
    /**
     * 품목 목록 조회 - GET /api/stocks/items
     * Entity → Map 변환으로 필요 필드만 선택
     */
    @GetMapping("/items")
    public ResponseEntity<List<Map<String, Object>>> getItems(
            @RequestParam(name = "useYn", defaultValue = "Y") String useYn) {
        
        try {
            System.out.println("📦 품목 목록 조회 요청 - useYn: " + useYn);
            
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
                .collect(Collectors.toList());
                
            System.out.println("✅ 품목 목록 조회 성공: " + result.size() + "개");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ 품목 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }
 
    /**
     * 엑셀 다운로드 - GET /api/stocks/excel
     * UTF-8 BOM 추가로 한글 깨짐 해결
     */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "whs", required = false) String whs,
            @RequestParam(name = "itemType", required = false) String itemType,
            @RequestParam(name = "stockStatus", required = false) String stockStatus) {
        
        try {
            System.out.println("📊 엑셀 다운로드 요청");
            
            // 1. JPA로 전체 데이터 조회 (기존 Service 그대로 활용)
            Pageable allData = PageRequest.of(0, 10000); // 충분히 큰 사이즈
            Page<StockResponseDTO> stocks = stockSvc.getList(keyword, whs, itemType, stockStatus, allData);
            
            // 2. UTF-8 BOM 추가해서 한글 깨짐 해결
            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF"); // UTF-8 BOM - 엑셀에서 한글 제대로 인식
            
            // 3. CSV 헤더 (LOT 번호 컬럼 제거)
            csv.append("순번,품목코드,품목명,품목유형,창고명,현재수량,단위,최종입고일,최종출고일\n");
            
            // 4. 순번 자동 생성 (1부터 시작)
            int seq = 1;
            for (StockResponseDTO stock : stocks.getContent()) {
                csv.append(seq++).append(",") // 순번 자동 증가
                   .append(stock.getItemCode() != null ? stock.getItemCode() : "").append(",")
                   .append(stock.getItemName() != null ? stock.getItemName() : "").append(",")
                   .append(stock.getItemType() != null ? stock.getItemType() : "").append(",")
                   .append(stock.getWarehouseName() != null ? stock.getWarehouseName() : "").append(",")
                   .append(stock.getQuantity() != null ? stock.getQuantity() : 0).append(",")
                   .append(stock.getUnit() != null ? stock.getUnit() : "").append(",")
                   .append(stock.getLastInDate() != null ? stock.getFormattedLastInDate() : "").append(",")
                   .append(stock.getLastOutDate() != null ? stock.getFormattedLastOutDate() : "").append("\n");
            }
            
            // 5. 응답 헤더 수정 - 한글 파일명 지원
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/vnd.ms-excel; charset=UTF-8");
            
            // 한글 파일명 인코딩
            String fileName = "재고현황_" + java.time.LocalDate.now() + ".csv";
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
                                                       .replaceAll("\\+", "%20");
            headers.add("Content-Disposition", 
                       "attachment; filename*=UTF-8''" + encodedFileName);
            
            System.out.println("✅ 엑셀 다운로드 완료 - " + stocks.getTotalElements() + "건");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv.toString().getBytes("UTF-8"));
                    
        } catch (Exception e) {
            System.err.println("❌ 엑셀 다운로드 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("오류 발생".getBytes());
        }
    }
    
    /**
     * 🆕 긴급출고용 거래처 목록 조회 - GET /api/customers
     */
    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> getCustomers(
            @RequestParam(name = "useYn", defaultValue = "Y") String useYn) {
        
        try {
            System.out.println("🏢 긴급출고용 거래처 목록 API 요청 - useYn: " + useYn);
            
            // StockBusinessService의 getActiveCustomers 메서드 호출
            List<Map<String, Object>> customers = stockBiz.getActiveCustomers();
            
            System.out.println("✅ 거래처 목록 조회 성공: " + customers.size() + "개");
            return ResponseEntity.ok(customers);
            
        } catch (Exception e) {
            System.err.println("❌ 거래처 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
            
            // 실패해도 기본 거래처는 제공
            List<Map<String, Object>> defaultCustomers = List.of(
                Map.of(
                    "id", 0L, 
                    "customerCode", "DEFAULT", 
                    "customerName", "기본 거래처",
                    "companyType", "CUSTOMER"
                )
            );
            
            return ResponseEntity.ok(defaultCustomers);
        }
    }
    
    

/**
 * 🆕 긴급출고 처리 - POST /api/stocks/emergency-out
 */
@PostMapping("/emergency-out")
public ResponseEntity<Map<String, String>> processQuickOut(@RequestBody QuickOutRequestDTO req) {
    
    try {
        System.out.println("🚨 긴급출고 API 요청: " + req);
        
        // 기본 유효성 검증
        if (!req.isValid()) {
            throw new IllegalArgumentException("필수 입력값이 누락되었습니다.");
        }
        
        // StockBusinessService의 quickOut 메서드 호출
        stockBiz.quickOut(req);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "긴급 출고가 완료되었습니다.");
        
        System.out.println("✅ 긴급출고 처리 완료");
        return ResponseEntity.ok(response);
        
    } catch (IllegalArgumentException e) {
        System.err.println("❌ 긴급출고 유효성 오류: " + e.getMessage());
        return createErrorResponse(e.getMessage());
        
    } catch (RuntimeException e) {
        System.err.println("❌ 긴급출고 비즈니스 오류: " + e.getMessage());
        return createErrorResponse(e.getMessage());
        
    } catch (Exception e) {
        System.err.println("❌ 긴급출고 시스템 오류: " + e.getMessage());
        e.printStackTrace();
        return createErrorResponse("긴급출고 처리 중 오류가 발생했습니다: " + e.getMessage());
    }
}
    
    
    // === 내부 헬퍼 메서드들 ===
    
    /**
     * 정렬 필드 유효성 검증 및 매핑
     */
    private String validateAndMapSortField(String sortField) {
        // 허용된 정렬 필드 목록
        return switch (sortField) {
            case "id" -> "id";
            case "itemCode" -> "item.code";
            case "itemName" -> "item.name";
            case "warehouseName" -> "warehouse.warehouseName";
            case "quantity" -> "quantity";
            case "warehouse.warehouseName" -> "warehouse.warehouseName"; // JS에서 매핑된 것
            default -> "id"; // 기본값
        };
    }
    
    /**
     * 창고 이동 요청 유효성 검증
     */
    private void validateTransferRequest(StockTransferRequestDTO req) {
        if (req.getFromWarehouseId() == null) {
            throw new IllegalArgumentException("출발 창고가 선택되지 않았습니다.");
        }
        
        if (req.getToWarehouseId() == null) {
            throw new IllegalArgumentException("도착 창고가 선택되지 않았습니다.");
        }
        
        if (req.getFromWarehouseId().equals(req.getToWarehouseId())) {
            throw new IllegalArgumentException("출발 창고와 도착 창고가 같을 수 없습니다.");
        }
        
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("이동 수량은 0보다 커야 합니다.");
        }
        
        if (req.getItemId() == null) {
            throw new IllegalArgumentException("품목 정보가 없습니다.");
        }
    }
    
    /**
     * 에러 응답 생성 헬퍼
     */
    private ResponseEntity<Map<String, String>> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }
}