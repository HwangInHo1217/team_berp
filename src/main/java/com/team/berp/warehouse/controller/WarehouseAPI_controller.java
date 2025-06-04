package com.team.berp.warehouse.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import com.team.berp.domain.WarehouseType;
import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.service.StockService;
import com.team.berp.warehouse.dto.WarehouseCreateRequestDTO;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.service.Warehouse_service;
import com.team.berp.warehouse.util.ApiUtils;

/**
 * WarehouseAPI_controller
 * 
 * 창고 관리 REST API 컨트롤러 클래스.
 * 등록, 수정, 삭제, 단건 조회, 검색, 페이징 조회, 코드 생성, 중복 확인 등
 * 창고와 관련된 모든 API 요청을 처리함.
 */
@RestController
@RequestMapping("/api/warehouses")
public class WarehouseAPI_controller {

    private static final Logger log = LoggerFactory.getLogger(WarehouseAPI_controller.class);
    private final Warehouse_service whsService;
    private final StockService stockService; // 🔧 StockService 의존성 주입 추가

    @Autowired
    public WarehouseAPI_controller(Warehouse_service whsService, StockService stockService) {
        this.whsService = whsService;
        this.stockService = stockService; // 🔧 생성자에 stockService 추가
    }

    /**
     * [GET] /generate-code
     * 
     * 창고 유형(RAW, PRODUCT 등)에 따라 자동으로 창고 코드를 생성하여 반환
     * 예: RAW → RWWH0001, PRODUCT → PDWH0001
     */
    @GetMapping("/generate-code")
    public ResponseEntity<String> generateWhsCode(@RequestParam("type") String type) {
        return ApiUtils.handle(() -> whsService.generateWhsCode(WarehouseType.valueOf(type.toUpperCase())));
    }

    /**
     * [GET] /check-duplicate
     * 
     * 창고 코드의 중복 여부 확인
     * - 등록 시 중복된 코드가 있는지 검사
     * - 수정 시 본인의 ID는 제외하고 중복 확인 가능 (excludeId 사용)
     */
    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, String>> checkWhsCodeDuplicate(
            @RequestParam("warehouseCode") String code,
            @RequestParam(value = "excludeId", required = false) Long excludeId) {

        return ResponseEntity.ok(whsService.checkWhsCodeDuplicate(code, excludeId));
    }

    /**
     * [POST] /
     * 
     * 새 창고 등록
     * - 필수 값: 창고명, 창고코드, 창고유형, 사용여부
     * - 코드 중복 및 형식 검증 포함
     */
    @PostMapping
    public ResponseEntity<?> createWhs(@RequestBody WarehouseCreateRequestDTO dto) {
        log.info("창고 등록 요청: {}", dto.getWarehouseName());
        return ApiUtils.handleCreate(() -> {
            var result = whsService.createWhs(dto);
            log.info("창고 등록 성공: {}", result.getWarehouseCode());
            return result;
        });
    }

    /**
     * [PUT] /{id}
     * 
     * 기존 창고 정보 수정
     * - ID로 대상 창고 조회 후 수정
     * - 수정 시 코드 중복 검증 및 유형-코드 형식 일치 여부 확인
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWhs(@PathVariable("id") Long id,
                                       @RequestBody WarehouseCreateRequestDTO dto) {
        log.info("창고 수정 요청, ID: {}", id);
        return ApiUtils.handle(() -> {
            var result = whsService.updateWhs(id, dto);
            log.info("창고 수정 성공: {}", result.getWarehouseCode());
            return result;
        });
    }

    /**
     * [GET] /
     * 
     * 창고 목록 조회 - 페이징 지원
     * - 검색어(keyword)와 검색유형(searchType)이 존재하면 전체 검색
     * - 아니면 사용여부 필터(useYnFilter)에 따라 조건부 조회
     * - page 파라미터로 페이징 처리 (1부터 시작)
     */
    @GetMapping
    public ResponseEntity<?> getAllWhs(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "searchType", required = false) String searchType,
            @RequestParam(name = "useYnFilter", required = false) String useYnFilter,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page) {

        return ApiUtils.handle(() ->
            StringUtils.hasText(keyword)
                ? whsService.searchWhsFromAllWithPaging(keyword, searchType, page)
                : whsService.getWhsByFilterWithPaging(useYnFilter, page)
        );
    }

    /**
     * [GET] /all
     * 
     * 창고 목록 조회 - 페이징 없이 전체 조회 (기존 JS에서 사용하던 방식과의 호환용)
     * - 검색어가 있으면 검색, 없으면 필터 기준 전체 목록 반환
     */
    @GetMapping("/all")
    public ResponseEntity<List<WarehouseResponseDTO>> getAllWhsNoPaging(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "searchType", required = false) String searchType,
            @RequestParam(name = "useYnFilter", required = false) String useYnFilter) {

        return ApiUtils.handle(() -> {
            var result = StringUtils.hasText(keyword)
                ? whsService.searchWhsFromAll(keyword, searchType)
                : whsService.getWhsByFilter(useYnFilter);
            return result.isEmpty() ? null : result;
        });
    }

    /**
     * [GET] /{id}
     * 
     * 특정 ID를 가진 창고의 상세 정보 조회
     * - 수정 모달 진입 시 단건 조회에 사용 가능
     */
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> getWhsById(@PathVariable("id") Long id) {
        return ApiUtils.handleGetById(() -> whsService.getWhsById(id));
    }

    /**
     * [DELETE] /{id}
     * 
     * ID에 해당하는 창고 삭제
     * - 단일 삭제만 지원
     * - 다중 삭제는 프론트에서 여러 DELETE 요청을 보내 처리
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWhs(@PathVariable("id") Long id) {
        return ApiUtils.handleDelete(() -> whsService.deleteWhs(id));
    }
    
    /**
     * 특정 창고의 재고 목록 조회 - GET /api/warehouses/{warehouseId}/stocks
     * 창고별 재고 모달에서 사용
     */
    @GetMapping("/{warehouseId}/stocks")
    public ResponseEntity<Page<StockResponseDTO>> getWarehouseStocks(
            @PathVariable("warehouseId") Long warehouseId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "itemType", required = false) String itemType,
            @RequestParam(name = "stockStatus", required = false) String stockStatus,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "item.name,ASC") String sort) {
        
        return ApiUtils.handle(() -> {
            log.debug("창고별 재고 조회 요청 - warehouseId: {}", warehouseId);
            
            // 창고 존재 여부 확인
            WarehouseResponseDTO warehouse = whsService.getWhsById(warehouseId);
            if (warehouse == null) {
                throw new IllegalArgumentException("존재하지 않는 창고입니다.");
            }
            
            // 창고 코드로 재고 검색 (기존 StockService 활용)
            String warehouseCode = warehouse.getWarehouseCode();
            
            // 정렬 조건 파싱
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && "ASC".equalsIgnoreCase(sortParams[1]) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            
            // 정렬 필드 매핑
            sortField = mapSortField(sortField);
            
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
            
            // 🔧 StockService 인스턴스 메서드로 호출 (static 제거)
            Page<StockResponseDTO> stocks = stockService.getList(
                keyword, warehouseCode, itemType, stockStatus, pageable);
            
            log.info("창고별 재고 조회 완료 - 창고: {}, 재고 수: {}", 
                    warehouse.getWarehouseName(), stocks.getTotalElements());
            
            return stocks;
        });
    }
    
    /**
     * 창고별 재고 요약 통계 - GET /api/warehouses/{warehouseId}/stocks/summary
     */
    @GetMapping("/{warehouseId}/stocks/summary")
    public ResponseEntity<Map<String, Object>> getWarehouseStockSummary(
            @PathVariable("warehouseId") Long warehouseId) {
        
        return ApiUtils.handle(() -> {
            log.debug("창고별 재고 요약 조회 - warehouseId: {}", warehouseId);
            
            // 창고 존재 여부 확인
            WarehouseResponseDTO warehouse = whsService.getWhsById(warehouseId);
            if (warehouse == null) {
                throw new IllegalArgumentException("존재하지 않는 창고입니다.");
            }
            
            // 🔧 StockService 인스턴스 메서드로 호출 (static 제거)
            Map<String, Object> summary = stockService.getWarehouseStockSummary(warehouseId);
            
            log.info("창고별 재고 요약 조회 완료 - 창고: {}", warehouse.getWarehouseName());
            
            return summary;
        });
    }

    /**
     * 정렬 필드 매핑 헬퍼 메서드
     */
    private String mapSortField(String sortField) {
        return switch (sortField) {
            case "itemCode" -> "item.code";
            case "itemName" -> "item.name";
            case "item.name" -> "item.name";
            case "item.code" -> "item.code";
            case "quantity" -> "quantity";
            case "warehouseName" -> "warehouse.warehouseName";
            default -> "item.name"; // 기본값: 품목명 정렬
        };
    }
}