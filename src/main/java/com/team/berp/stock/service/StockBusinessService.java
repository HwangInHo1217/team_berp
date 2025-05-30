// ===== StockBusinessService.java (수정된 버전) =====
package com.team.berp.stock.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.domain.LogType;
import com.team.berp.domain.Stock;
import com.team.berp.domain.Warehouse;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.repository.StockRepository;
import com.team.berp.warehouse.repository.Warehouse_repository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockBusinessService {
    
    private final StockRepository stockRepo;
    private final ItemRepository itemRepo;
    private final Warehouse_repository whsRepo;
    private final StockValidationService validationSvc;
    private final StockUpdateService updateSvc;
    private final InventoryLogService logSvc;
    private final InventoryLogRepository logRepo;
    
    /**
     * 🔍 재고 검색 로직 (복합 필터 지원) - 수정된 버전
     * 모든 조건을 조합하여 검색하되, stockStatus는 마지막에 추가 필터링으로 적용
     */
    public Page<StockResponseDTO> searchStocks(String keyword, String whsCode, String itemType, 
                                              String stockStatus, Pageable page) {
        
        System.out.println("🔍 검색 파라미터:");
        System.out.println("  keyword: '" + keyword + "'");
        System.out.println("  whsCode: '" + whsCode + "'");
        System.out.println("  itemType: '" + itemType + "'");
        System.out.println("  stockStatus: '" + stockStatus + "'");
        
        Page<Stock> stocks;
        
        try {
            // 🎯 1단계: 키워드 + 창고 + 품목유형 조건으로 기본 검색
            stocks = performBasicSearch(keyword, whsCode, itemType, page);
            
            // 🎯 2단계: stockStatus가 있으면 추가 필터링
            if (!isEmpty(stockStatus)) {
                stocks = applyStockStatusFilter(stocks, stockStatus, keyword, whsCode, itemType, page);
            }
            
            System.out.println("  📊 최종 검색 결과: " + stocks.getTotalElements() + "건");
            
            // 🔄 Entity → DTO 변환
            return stocks.map(stock -> {
                try {
                    StockResponseDTO dto = StockResponseDTO.fromEntity(stock);
                    
                    // 📅 최종 입고일/출고일 조회
                    try {
                        logRepo.findLastInDate(stock.getItem().getId(), stock.getWarehouse().getId())
                               .ifPresent(date -> {
                                   dto.setLastInDate(date);
                                   dto.setActualLastInAt(date);
                               });
                               
                        logRepo.findLastOutDate(stock.getItem().getId(), stock.getWarehouse().getId())
                               .ifPresent(date -> {
                                   dto.setLastOutDate(date);
                                   dto.setActualLastOutAt(date);
                               });
                    } catch (Exception e) {
                        System.err.println("로그 조회 오류: " + e.getMessage());
                    }
                    
                    return dto;
                } catch (Exception e) {
                    System.err.println("DTO 변환 오류: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("재고 데이터 변환 중 오류 발생", e);
                }
            });
            
        } catch (Exception e) {
            System.err.println("재고 검색 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("재고 검색 중 오류 발생", e);
        }
    }
    
    /**
     * 🔍 1단계: 기본 검색 (키워드 + 창고 + 품목유형)
     */
    private Page<Stock> performBasicSearch(String keyword, String whsCode, String itemType, Pageable page) {
        
        if (!isEmpty(keyword) && !isEmpty(whsCode) && !isEmpty(itemType)) {
            // 키워드 + 창고 + 품목유형 조합
            ItemType itemTypeEnum = parseItemType(itemType);
            if (itemTypeEnum != null) {
                System.out.println("  → 키워드+창고+품목유형 조합 검색");
                return stockRepo.searchByKeywordAndWarehouseAndItemType(keyword, whsCode, itemTypeEnum, page);
            } else {
                System.out.println("  → itemType 변환 실패, 키워드+창고 검색");
                return stockRepo.searchByKeywordAndWarehouse(keyword, whsCode, page);
            }
            
        } else if (!isEmpty(keyword) && !isEmpty(whsCode)) {
            // 키워드 + 창고 조합
            System.out.println("  → 키워드+창고 조합 검색");
            return stockRepo.searchByKeywordAndWarehouse(keyword, whsCode, page);
            
        } else if (!isEmpty(keyword) && !isEmpty(itemType)) {
            // 키워드 + 품목유형 조합
            ItemType itemTypeEnum = parseItemType(itemType);
            if (itemTypeEnum != null) {
                System.out.println("  → 키워드+품목유형 조합 검색");
                return stockRepo.searchByKeywordAndItemType(keyword, itemTypeEnum, page);
            } else {
                System.out.println("  → itemType 변환 실패, 키워드만 검색");
                return stockRepo.searchByKeyword(keyword.trim(), page);
            }
            
        } else if (!isEmpty(whsCode) && !isEmpty(itemType)) {
            // 창고 + 품목유형 조합
            ItemType itemTypeEnum = parseItemType(itemType);
            if (itemTypeEnum != null) {
                System.out.println("  → 창고+품목유형 조합 검색");
                return stockRepo.findByItemTypeAndWarehouseCode(itemTypeEnum, whsCode, page);
            } else {
                System.out.println("  → itemType 변환 실패, 창고만 검색");
                return stockRepo.findByWarehouseCode(whsCode, page);
            }
            
        } else if (!isEmpty(keyword)) {
            // 키워드만 검색
            System.out.println("  → 키워드만 검색");
            return stockRepo.searchByKeyword(keyword.trim(), page);
            
        } else if (!isEmpty(whsCode)) {
            // 창고만 검색
            System.out.println("  → 창고만 검색");
            return stockRepo.findByWarehouseCode(whsCode, page);
            
        } else if (!isEmpty(itemType)) {
            // 품목유형만 검색
            ItemType itemTypeEnum = parseItemType(itemType);
            if (itemTypeEnum != null) {
                System.out.println("  → 품목유형만 검색");
                return stockRepo.findByItemType(itemTypeEnum, page);
            } else {
                System.out.println("  → itemType 변환 실패, 전체 조회");
                return stockRepo.findAll(page);
            }
            
        } else {
            // 조건 없음 - 전체 조회
            System.out.println("  → 전체 조회");
            return stockRepo.findAll(page);
        }
    }
    
    /**
     * 🔍 2단계: 재고상태 필터링 적용
     * 기존 검색 결과에 재고상태 조건을 추가로 적용
     */
    private Page<Stock> applyStockStatusFilter(Page<Stock> basicResult, String stockStatus, 
                                              String keyword, String whsCode, String itemType, Pageable page) {
        
        System.out.println("  → 재고상태 필터링 적용: " + stockStatus);
        
        // ⭐ 핵심: 기존 조건을 유지하면서 재고상태 조건을 추가한 새로운 검색
        switch (stockStatus) {
            case "inStock":
                return performBasicSearchWithStockFilter(keyword, whsCode, itemType, "inStock", page);
            case "outOfStock":
                return performBasicSearchWithStockFilter(keyword, whsCode, itemType, "outOfStock", page);
            case "belowSafety":
                return performBasicSearchWithStockFilter(keyword, whsCode, itemType, "belowSafety", page);
            default:
                return basicResult; // 알 수 없는 상태면 기본 결과 그대로 반환
        }
    }
    
    /**
     * 🔍 재고상태 조건을 포함한 통합 검색
     * 모든 조건(키워드+창고+품목유형+재고상태)을 한번에 적용
     */
    private Page<Stock> performBasicSearchWithStockFilter(String keyword, String whsCode, 
                                                         String itemType, String stockStatus, Pageable page) {
        
        // 📝 새로운 Repository 메서드들이 필요합니다!
        // 현재 Repository에는 모든 조건을 조합한 메서드가 부족합니다.
        
        if (!isEmpty(keyword) && !isEmpty(whsCode) && !isEmpty(itemType)) {
            // 키워드 + 창고 + 품목유형 + 재고상태 (4개 조건 모두)
            ItemType itemTypeEnum = parseItemType(itemType);
            if (itemTypeEnum != null) {
                return stockRepo.searchByAllConditions(keyword, whsCode, itemTypeEnum, stockStatus, page);
            }
        } else if (!isEmpty(whsCode) && !isEmpty(itemType)) {
            // 창고 + 품목유형 + 재고상태
            ItemType itemTypeEnum = parseItemType(itemType);
            if (itemTypeEnum != null) {
                return stockRepo.findByItemTypeAndWarehouseCodeAndStockStatus(itemTypeEnum, whsCode, stockStatus, page);
            }
        } else if (!isEmpty(keyword) && !isEmpty(whsCode)) {
            // 키워드 + 창고 + 재고상태
            return stockRepo.searchByKeywordAndWarehouseAndStockStatus(keyword, whsCode, stockStatus, page);
        } else if (!isEmpty(keyword) && !isEmpty(itemType)) {
            // 키워드 + 품목유형 + 재고상태
            ItemType itemTypeEnum = parseItemType(itemType);
            if (itemTypeEnum != null) {
                return stockRepo.searchByKeywordAndItemTypeAndStockStatus(keyword, itemTypeEnum, stockStatus, page);
            }
        } else if (!isEmpty(keyword)) {
            // 키워드 + 재고상태
            return stockRepo.searchByKeywordAndStockStatus(keyword, stockStatus, page);
        } else if (!isEmpty(whsCode)) {
            // 창고 + 재고상태
            return stockRepo.findByWarehouseCodeAndStockStatus(whsCode, stockStatus, page);
        } else if (!isEmpty(itemType)) {
            // 품목유형 + 재고상태
            ItemType itemTypeEnum = parseItemType(itemType);
            if (itemTypeEnum != null) {
                return stockRepo.findByItemTypeAndStockStatus(itemTypeEnum, stockStatus, page);
            }
        }
        
        // 재고상태만
        return switch (stockStatus) {
            case "inStock" -> stockRepo.findInStock(page);
            case "outOfStock" -> stockRepo.findOutOfStock(page);
            case "belowSafety" -> stockRepo.findBelowSafety(page);
            default -> stockRepo.findAll(page);
        };
    }
    
    // === 나머지 메서드들 (기존과 동일) ===
    
    public StockResponseDTO getStockDetail(Long stockId) {
        try {
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new RuntimeException("재고 정보를 찾을 수 없습니다."));
            
            StockResponseDTO dto = StockResponseDTO.fromEntity(stock);
            
            try {
                logRepo.findLastInDate(stock.getItem().getId(), stock.getWarehouse().getId())
                       .ifPresent(date -> {
                           dto.setLastInDate(date);
                           dto.setActualLastInAt(date);
                       });
                       
                logRepo.findLastOutDate(stock.getItem().getId(), stock.getWarehouse().getId())
                       .ifPresent(date -> {
                           dto.setLastOutDate(date);
                           dto.setActualLastOutAt(date);
                       });
            } catch (Exception e) {
                System.err.println("로그 조회 오류: " + e.getMessage());
            }
            
            return dto;
        } catch (Exception e) {
            System.err.println("재고 상세 조회 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("재고 상세 조회 중 오류 발생", e);
        }
    }
    
    @Transactional
    public void stockIn(StockRequestDTO req) {
        validationSvc.validate(req);
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        Optional<Stock> stock = stockRepo.findByItemAndWarehouse(item, whs);
        
        if (stock.isPresent()) {
            updateSvc.addQty(stock.get(), req.getQuantity());
        } else {
            updateSvc.createStock(req, item, whs);
        }
        
        logSvc.createLog(LogType.IN, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "입고 처리");
    }
    
    @Transactional
    public void stockOut(StockRequestDTO req) {
        validationSvc.validate(req);
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        Stock stock = stockRepo.findByItemAndWarehouse(item, whs)
                .orElseThrow(() -> new RuntimeException("출고할 재고가 없습니다."));
        
        validationSvc.checkQty(stock, req.getQuantity());
        updateSvc.subQty(stock, req.getQuantity());
        
        logSvc.createLog(LogType.OUT, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "출고 처리");
    }
    
    @Transactional
    public void dispose(StockRequestDTO req) {
        validationSvc.validate(req);
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        Stock stock = stockRepo.findByItemAndWarehouse(item, whs)
                .orElseThrow(() -> new RuntimeException("폐기할 재고가 없습니다."));
        
        validationSvc.checkQty(stock, req.getQuantity());
        updateSvc.subQty(stock, req.getQuantity());
        
        logSvc.createLog(LogType.DISPOSE, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "폐기 처리");
    }
    
    @Transactional
    public void returnIn(StockRequestDTO req) {
        validationSvc.validate(req);
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        Optional<Stock> stock = stockRepo.findByItemAndWarehouse(item, whs);
        
        if (stock.isPresent()) {
            updateSvc.addQty(stock.get(), req.getQuantity());
        } else {
            updateSvc.createStock(req, item, whs);
        }
        
        logSvc.createLog(LogType.RETURN_IN, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "반품입고 처리");
    }
    
    // === 유틸리티 메서드들 ===
    
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    private ItemType parseItemType(String itemTypeStr) {
        if (isEmpty(itemTypeStr)) return null;
        
        try {
            return ItemType.valueOf(itemTypeStr);
        } catch (IllegalArgumentException e) {
            System.err.println("유효하지 않은 품목 유형: " + itemTypeStr);
            return null;
        }
    }
    
    private Item getItem(Long itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("품목을 찾을 수 없습니다."));
    }
    
    private Warehouse getWarehouse(Long warehouseId) {
        return whsRepo.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("창고를 찾을 수 없습니다."));
    }
}