// ===== StockBusinessService.java (수정된 버전) =====
package com.team.berp.stock.service;

import java.util.HashMap;
import java.util.Map;
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
import com.team.berp.stock.dto.StockTransferRequestDTO;
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
    
    
 // ===== StockBusinessService.java에 추가할 메서드들 =====

    /**
     * 🔄 창고간 재고 이동 처리
     */
    @Transactional
    public void transferStock(StockTransferRequestDTO req) {
        
        System.out.println("🔄 창고 이동 시작: " + req);
        
        // 1. 유효성 검사
        validateTransferRequest(req);
        
        // 2. 필요한 엔티티들 조회
        Item item = getItem(req.getItemId());
        Warehouse fromWhs = getWarehouse(req.getFromWarehouseId());
        Warehouse toWhs = getWarehouse(req.getToWarehouseId());
        
        // 3. 출발지 재고 확인 및 차감
        Stock fromStock = stockRepo.findByItemAndWarehouse(item, fromWhs)
                .orElseThrow(() -> new RuntimeException("출발지에 해당 재고가 없습니다."));
        
        validationSvc.checkQty(fromStock, req.getQuantity());
        
        System.out.println("✅ 출발지 재고 확인 완료: " + fromStock.getQuantity() + "개");
        
        // 4. 출발지 재고 차감
        updateSvc.subQty(fromStock, req.getQuantity());
        
        // 5. 도착지 재고 증가 (없으면 생성)
        Optional<Stock> toStockOpt = stockRepo.findByItemAndWarehouse(item, toWhs);
        
        if (toStockOpt.isPresent()) {
            // 기존 재고가 있으면 수량 추가
            updateSvc.addQty(toStockOpt.get(), req.getQuantity());
            System.out.println("✅ 기존 재고에 추가: " + toStockOpt.get().getQuantity());
        } else {
            // 새 재고 생성
            StockRequestDTO newStockReq = new StockRequestDTO();
            newStockReq.setItemId(req.getItemId());
            newStockReq.setWarehouseId(req.getToWarehouseId());
            newStockReq.setQuantity(req.getQuantity());
            newStockReq.setComment("창고간 이동으로 생성");
            
            updateSvc.createStock(newStockReq, item, toWhs);
            System.out.println("✅ 새로운 재고 생성 완료");
        }
        
        // 6. 이동 로그 기록
        String reasonText = getReasonText(req.getReason());
        String comment = String.format("[창고이동] %s → %s, 사유: %s", 
                                      fromWhs.getWarehouseName(), 
                                      toWhs.getWarehouseName(),
                                      reasonText);
        
        if (req.getComment() != null && !req.getComment().trim().isEmpty()) {
            comment += ", 비고: " + req.getComment();
        }
        
        // 출발지 출고 로그
        logSvc.createLog(LogType.OUT, item, fromWhs, req.getQuantity(), 
                        comment + " (출고)");
        
        // 도착지 입고 로그  
        logSvc.createLog(LogType.IN, item, toWhs, req.getQuantity(), 
                        comment + " (입고)");
        
        System.out.println("✅ 창고 이동 완료!");
    }

    /**
     * 📊 재고 현황 요약 통계
     */
    public Map<String, Object> getStockSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();
            
            // 전체 재고 품목 수 (중복 제거)
            long totalItems = stockRepo.countDistinctItems();
            
            // 재고 없는 품목 수
            long outOfStockItems = stockRepo.countByQuantity(0);
            
            // 안전재고 미달 품목 수 (1~9개)
            long belowSafetyItems = stockRepo.countByQuantityBetween(1, 9);
            
            // 정상 재고 품목 수
            long normalStockItems = Math.max(0, totalItems - outOfStockItems - belowSafetyItems);
            
            summary.put("totalItems", totalItems);
            summary.put("outOfStock", outOfStockItems); 
            summary.put("belowSafety", belowSafetyItems);
            summary.put("normalStock", normalStockItems);
            
            System.out.println("📊 재고 요약: " + summary);
            return summary;
            
        } catch (Exception e) {
            System.err.println("재고 요약 통계 조회 실패: " + e.getMessage());
            e.printStackTrace();
            
            // 실패시 기본값 반환
            Map<String, Object> defaultSummary = new HashMap<>();
            defaultSummary.put("totalItems", 0L);
            defaultSummary.put("outOfStock", 0L);
            defaultSummary.put("belowSafety", 0L);
            defaultSummary.put("normalStock", 0L);
            
            return defaultSummary;
        }
    }

    // === 내부 헬퍼 메서드들 ===

    /**
     * 🔍 창고 이동 요청 유효성 검사
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
     * 이동 사유 코드를 한글로 변환
     */
    private String getReasonText(String reasonCode) {
        if (reasonCode == null || reasonCode.trim().isEmpty()) {
            return "일반 이동";
        }
        
        return switch (reasonCode) {
            case "PRODUCTION" -> "생산 투입";
            case "SHIPPING_PREPARE" -> "출하 준비";
            case "REORGANIZATION" -> "창고 정리";
            case "MAINTENANCE" -> "창고 보수";
            case "OTHER" -> "기타";
            default -> reasonCode; // 그대로 표시
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
        
        logSvc.createLog(LogType.TRANSFER, item, whs, req.getQuantity(), 
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
        
        logSvc.createLog(LogType.TRANSFER, item, whs, req.getQuantity(), 
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