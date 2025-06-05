package com.team.berp.stock.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.client.repository.ClientRepository;
import com.team.berp.domain.Company;
import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.domain.LogType;
import com.team.berp.domain.Stock;
import com.team.berp.domain.Warehouse;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.stock.dto.QuickOutRequestDTO;
import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.dto.StockTransferRequestDTO;
import com.team.berp.stock.repository.StockRepository;
import com.team.berp.warehouse.repository.Warehouse_repository;

import lombok.RequiredArgsConstructor;

/**
 * 재고 비즈니스 로직 서비스
 * - 복잡한 재고 검색 및 필터링 로직 처리
 * - 창고간 이동, 입출고 등의 핵심 비즈니스 로직 구현
 * - 트랜잭션 처리 및 데이터 일관성 보장
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockBusinessService {
    
    private final StockRepository stockRepo;
    private final ItemRepository itemRepo;
    private final Warehouse_repository whsRepo;
    private final ClientRepository clientRepo; // 거래처 조회용 추가
    private final StockValidationService validationSvc;
    private final StockUpdateService updateSvc;
    private final InventoryLogService logSvc;
    private final InventoryLogRepository logRepo;
    
    /**
     * 재고 검색 로직 (복합 필터 지원)
     * 모든 조건을 조합하여 검색하되, stockStatus는 마지막에 추가 필터링으로 적용
     */
    public Page<StockResponseDTO> searchStocks(String keyword, String whsCode, String itemType, 
                                              String stockStatus, Pageable page) {
        
        System.out.println("🔍 재고 검색 파라미터:");
        System.out.println("  keyword: '" + keyword + "'");
        System.out.println("  whsCode: '" + whsCode + "'");
        System.out.println("  itemType: '" + itemType + "'");
        System.out.println("  stockStatus: '" + stockStatus + "'");
        
        Page<Stock> stocks;
        
        try {
            // 1단계: 키워드 + 창고 + 품목유형 조건으로 기본 검색
            stocks = performBasicSearch(keyword, whsCode, itemType, page);
            
            // 2단계: stockStatus가 있으면 추가 필터링
            if (!isEmpty(stockStatus)) {
                stocks = applyStockStatusFilter(stocks, stockStatus, keyword, whsCode, itemType, page);
            }
            
            System.out.println("  📊 최종 검색 결과: " + stocks.getTotalElements() + "건");
            
            // Entity → DTO 변환 (LOT 번호 제외)
            return stocks.map(stock -> {
                try {
                    StockResponseDTO dto = StockResponseDTO.fromEntity(stock);
                    
                    // 최종 입고일/출고일 조회 (LOT 정보는 제외)
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
     * 1단계: 기본 검색 (키워드 + 창고 + 품목유형)
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
     * 2단계: 재고상태 필터링 적용
     * 기존 검색 결과에 재고상태 조건을 추가로 적용
     */
    private Page<Stock> applyStockStatusFilter(Page<Stock> basicResult, String stockStatus, 
                                              String keyword, String whsCode, String itemType, Pageable page) {
        
        System.out.println("  → 재고상태 필터링 적용: " + stockStatus);
        
        // 기존 조건을 유지하면서 재고상태 조건을 추가한 새로운 검색
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
     * 재고상태 조건을 포함한 통합 검색
     * 모든 조건(키워드+창고+품목유형+재고상태)을 한번에 적용
     */
    private Page<Stock> performBasicSearchWithStockFilter(String keyword, String whsCode, 
                                                         String itemType, String stockStatus, Pageable page) {
        
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
    
 // StockBusinessService.java - transferStock 메서드 수정

    /**
     * 창고간 재고 이동 처리 (🔧 이력 기록 개선 버전)
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
        
        // 3. 품목 유형과 창고 유형 매칭 검증
        validateWarehouseTypeCompatibility(item, toWhs);
        
        // 4. 출발지 재고 확인 및 차감
        Stock fromStock;
        if (req.getFromStockId() != null) {
            fromStock = stockRepo.findById(req.getFromStockId())
                    .orElseThrow(() -> new RuntimeException("출발지 재고를 찾을 수 없습니다."));
        } else {
            List<Stock> stockList = stockRepo.findByItem(item).stream()
                    .filter(stock -> stock.getWarehouse().getId().equals(fromWhs.getId()))
                    .toList();
            
            if (stockList.isEmpty()) {
                throw new RuntimeException("출발지에 해당 재고가 없습니다.");
            } else if (stockList.size() > 1) {
                fromStock = stockList.stream()
                        .max((s1, s2) -> Integer.compare(s1.getQuantity(), s2.getQuantity()))
                        .orElseThrow(() -> new RuntimeException("출발지 재고 선택 중 오류가 발생했습니다."));
                System.out.println("⚠️ 중복 재고 발견, 수량이 많은 재고 선택: " + fromStock.getQuantity() + "개");
            } else {
                fromStock = stockList.get(0);
            }
        }
        
        validationSvc.checkQty(fromStock, req.getQuantity());
        System.out.println("✅ 출발지 재고 확인 완료: " + fromStock.getQuantity() + "개");
        
        // 5. 출발지 재고 차감
        updateSvc.subQty(fromStock, req.getQuantity());
        
        // 6. 도착지 재고 증가 (없으면 생성)
        List<Stock> toStockList = stockRepo.findByItem(item).stream()
                .filter(stock -> stock.getWarehouse().getId().equals(toWhs.getId()))
                .toList();
        
        if (!toStockList.isEmpty()) {
            Stock toStock = toStockList.get(0);
            updateSvc.addQty(toStock, req.getQuantity());
            System.out.println("✅ 기존 재고에 추가: " + toStock.getQuantity());
            
            if (toStockList.size() > 1) {
                System.out.println("⚠️ 도착지에 중복 재고 발견, 첫 번째 재고에만 추가");
            }
        } else {
            StockRequestDTO newStockReq = new StockRequestDTO();
            newStockReq.setItemId(req.getItemId());
            newStockReq.setWarehouseId(req.getToWarehouseId());
            newStockReq.setQuantity(req.getQuantity());
            newStockReq.setComment("창고간 이동으로 생성");
            
            updateSvc.createStock(newStockReq, item, toWhs);
            System.out.println("✅ 새로운 재고 생성 완료");
        }
        
        // 🆕 7. 개선된 이력 기록 - 출발지와 도착지 모두 기록
        String reasonText = getReasonText(req.getReason());
        
        // 🔧 출발지 이력 (기존과 동일)
        String fromComment = String.format("[창고이동-출고] %s → %s (수량: %d개), 사유: %s", 
                                          fromWhs.getWarehouseName(), 
                                          toWhs.getWarehouseName(),
                                          req.getQuantity(),
                                          reasonText);
        
        if (req.getComment() != null && !req.getComment().trim().isEmpty()) {
            fromComment += ", 비고: " + req.getComment();
        }
        
        // 🆕 도착지 이력 (새로 추가)
        String toComment = String.format("[창고이동-입고] %s ← %s (수량: %d개), 사유: %s", 
                                        toWhs.getWarehouseName(),
                                        fromWhs.getWarehouseName(), 
                                        req.getQuantity(),
                                        reasonText);
        
        if (req.getComment() != null && !req.getComment().trim().isEmpty()) {
            toComment += ", 비고: " + req.getComment();
        }
        
        // 🔧 출발지 OUT 로그 (기존)
        logSvc.createLog(LogType.OUT, item, fromWhs, req.getQuantity(), fromComment);
        
        // 🆕 도착지 IN 로그 (새로 추가) - IN 타입으로 기록
        logSvc.createLog(LogType.IN, item, toWhs, req.getQuantity(), toComment);
        
        System.out.println("✅ 창고 이동 완료! (출발지/도착지 모두 이력 기록됨)");
    }

    /**
     * 재고 현황 요약 통계
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

    /**
     * 재고 상세 조회
     */
    public StockResponseDTO getStockDetail(Long stockId) {
        try {
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new RuntimeException("재고 정보를 찾을 수 없습니다."));
            
            StockResponseDTO dto = StockResponseDTO.fromEntity(stock);
            
            // 최종 입출고일 조회
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
    
    /**
     * 재고 입고 처리 (중복 재고 문제 해결)
     */
    @Transactional
    public void stockIn(StockRequestDTO req) {
        validationSvc.validate(req);
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        // 🔧 중복 재고 문제 해결
        List<Stock> stockList = stockRepo.findByItem(item).stream()
                .filter(stock -> stock.getWarehouse().getId().equals(whs.getId()))
                .toList();
        
        if (!stockList.isEmpty()) {
            // 기존 재고가 있으면 첫 번째 재고에 추가
            updateSvc.addQty(stockList.get(0), req.getQuantity());
        } else {
            // 새 재고 생성
            updateSvc.createStock(req, item, whs);
        }
        
        logSvc.createLog(LogType.IN, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "입고 처리");
    }
    
    /**
     * 재고 출고 처리 (중복 재고 문제 해결)
     */
    @Transactional
    public void stockOut(StockRequestDTO req) {
        validationSvc.validate(req);
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        // 🔧 중복 재고 문제 해결
        List<Stock> stockList = stockRepo.findByItem(item).stream()
                .filter(stock -> stock.getWarehouse().getId().equals(whs.getId()))
                .toList();
        
        if (stockList.isEmpty()) {
            throw new RuntimeException("출고할 재고가 없습니다.");
        }
        
        // 수량이 충분한 재고 찾기
        Stock stock = stockList.stream()
                .filter(s -> s.getQuantity() >= req.getQuantity())
                .findFirst()
                .orElse(stockList.get(0)); // 수량이 부족해도 첫 번째 재고로 시도
        
        validationSvc.checkQty(stock, req.getQuantity());
        updateSvc.subQty(stock, req.getQuantity());
        
        logSvc.createLog(LogType.OUT, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "출고 처리");
    }
    

    // === 내부 헬퍼 메서드들 ===

    
    /**
     * 창고 이동 요청 유효성 검사
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
     * 품목 유형과 창고 유형 호환성 검증
     */
    private void validateWarehouseTypeCompatibility(Item item, Warehouse warehouse) {
        boolean isCompatible = false;
        String errorMessage = "";
        
        if (item.getType() == ItemType.product && warehouse.getWarehouseType().name().equals("PRODUCT")) {
            isCompatible = true;
        } else if (item.getType() == ItemType.raw && warehouse.getWarehouseType().name().equals("RAW")) {
            isCompatible = true;
        } else if (item.getType() == ItemType.product && warehouse.getWarehouseType().name().equals("RAW")) {
            errorMessage = "완제품은 자재 창고로 이동할 수 없습니다.";
        } else if (item.getType() == ItemType.raw && warehouse.getWarehouseType().name().equals("PRODUCT")) {
            errorMessage = "자재는 완제품 창고로 이동할 수 없습니다.";
        } else {
            errorMessage = "품목 유형에 맞는 창고를 선택해주세요.";
        }
        
        if (!isCompatible) {
            throw new IllegalArgumentException(errorMessage);
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
    
    /**
     * 🆕 긴급출고용 거래처 목록 조회 (개선된 버전)
     */
    public List<Map<String, Object>> getActiveCustomers() {
        try {
            System.out.println("🏢 긴급출고용 거래처 목록 조회 시작");
            
            // ClientRepository 활용해서 모든 회사 조회
            List<Company> allCompanies = clientRepo.findAll();
            System.out.println("📊 전체 회사 수: " + allCompanies.size());
            
            // CUSTOMER 또는 BOTH 타입이면서 활성 상태인 회사만 필터링
            List<Company> customers = allCompanies.stream()
                .filter(company -> {
                    // useYn이 'Y'이고
                    boolean isActive = "Y".equals(company.getUseYn());
                    
                    // companyType이 CUSTOMER 또는 BOTH인 경우만
                    boolean isCustomer = Company.CompanyType.CUSTOMER.equals(company.getCompanyType()) || 
                                       Company.CompanyType.BOTH.equals(company.getCompanyType());
                    
                    System.out.println("🔍 회사 검토: " + company.getCompanyName() + 
                                     ", 고객사: " + isCustomer + 
                                     ", 타입: " + company.getCompanyType());
                    
                    return isActive && isCustomer;
                })
                .collect(Collectors.toList());
            
            System.out.println("📋 필터링된 거래처 수: " + customers.size());
            
            // Company → Map 변환 (JavaScript 호환)
            List<Map<String, Object>> result = customers.stream()
                .map(company -> {
                    Map<String, Object> customerMap = new HashMap<>();
                    
                    // JavaScript에서 기대하는 필드명으로 매핑
                    customerMap.put("id", company.getCompanyId());
                    customerMap.put("customerCode", 
                        company.getCustCd() != null ? company.getCustCd() : "CUST" + company.getCompanyId());
                    customerMap.put("customerName", company.getCompanyName());
                    customerMap.put("companyType", company.getCompanyType().name());
                    customerMap.put("presidentNm", company.getPresidentNm());
                    customerMap.put("companyTel", company.getCompanyTel());
                    customerMap.put("companyAddr", company.getMainAddress());
                    
                    System.out.println("✨ 거래처 매핑: " + customerMap.get("customerName") + 
                                     " (ID: " + customerMap.get("id") + ")");
                    
                    return customerMap;
                })
                .collect(Collectors.toList());
            
            System.out.println("✅ 긴급출고용 거래처 조회 완료: " + result.size() + "개");
            
            // 빈 리스트일 경우 기본 거래처 추가
            if (result.isEmpty()) {
                Map<String, Object> defaultCustomer = new HashMap<>();
                defaultCustomer.put("id", 0L);
                defaultCustomer.put("customerCode", "DEFAULT");
                defaultCustomer.put("customerName", "기본 거래처");
                defaultCustomer.put("companyType", "CUSTOMER");
                result.add(defaultCustomer);
                
                System.out.println("⚠️ 활성 거래처가 없어 기본 거래처 추가");
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 거래처 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
            
            // 예외 발생시 기본 거래처만 반환
            List<Map<String, Object>> fallbackResult = new ArrayList<>();
            Map<String, Object> defaultCustomer = new HashMap<>();
            defaultCustomer.put("id", 0L);
            defaultCustomer.put("customerCode", "DEFAULT");
            defaultCustomer.put("customerName", "기본 거래처");
            defaultCustomer.put("companyType", "CUSTOMER");
            fallbackResult.add(defaultCustomer);
            
            return fallbackResult;
        }
    }
    
    /**
     * 🆕 긴급출고 처리 (거래처 정보 포함)
     */
    @Transactional
    public void quickOut(QuickOutRequestDTO req) {
        try {
            System.out.println("🚨 긴급출고 처리 시작: " + req);
            
            // 기본 유효성 검증
            validateQuickOutRequest(req);
            
            // 필요한 엔티티들 조회
            Item item = getItem(req.getItemId());
            Warehouse whs = getWarehouse(req.getWarehouseId());
            
            // 거래처 정보 조회 (선택사항)
            Company company = null;
            if (req.getCompanyId() != null && req.getCompanyId() > 0) {
                company = getCompany(req.getCompanyId());
            }
            
            // 🔧 재고 확인 및 차감 (중복 재고 문제 해결)
            List<Stock> stockList = stockRepo.findByItem(item).stream()
                    .filter(stock -> stock.getWarehouse().getId().equals(whs.getId()))
                    .toList();
            
            if (stockList.isEmpty()) {
                throw new RuntimeException("출고할 재고가 없습니다.");
            }
            
            // 수량이 충분한 재고 찾기
            Stock stock = stockList.stream()
                    .filter(s -> s.getQuantity() >= req.getQuantity())
                    .findFirst()
                    .orElse(stockList.get(0)); // 수량이 부족해도 첫 번째 재고로 시도
            
            validationSvc.checkQty(stock, req.getQuantity());
            updateSvc.subQty(stock, req.getQuantity());
            
            // 출고 로그 생성 (거래처 정보 포함)
            String comment = createQuickOutComment(req, company);
            logSvc.createLog(LogType.OUT, item, whs, req.getQuantity(), comment);
            
            System.out.println("✅ 긴급출고 완료: " + req.getQuantity() + "개");
            
        } catch (Exception e) {
            System.err.println("❌ 긴급출고 처리 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("긴급출고 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 🆕 긴급출고 코멘트 생성
     */
    private String createQuickOutComment(QuickOutRequestDTO req, Company company) {
        StringBuilder comment = new StringBuilder();
        comment.append("[긴급출고]");
        
        if (company != null) {
            comment.append(" 거래처: ").append(company.getCompanyName());
            if (company.getPresidentNm() != null) {
                comment.append(" (").append(company.getPresidentNm()).append(")");
            }
        }
        
        if (req.getReason() != null && !req.getReason().trim().isEmpty()) {
            comment.append(", 사유: ").append(req.getReason());
        }
        
        if (req.getComment() != null && !req.getComment().trim().isEmpty()) {
            comment.append(", 비고: ").append(req.getComment());
        }
        
        return comment.toString();
    }
    
    /**
     * 🆕 긴급출고 요청 유효성 검증
     */
    private void validateQuickOutRequest(QuickOutRequestDTO req) {
        if (req.getItemId() == null) {
            throw new IllegalArgumentException("품목 정보가 없습니다.");
        }
        
        if (req.getWarehouseId() == null) {
            throw new IllegalArgumentException("창고 정보가 없습니다.");
        }
        
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("출고 수량은 0보다 커야 합니다.");
        }
        
        if (req.getQuantity() > 10) {
            throw new IllegalArgumentException("긴급출고는 최대 10개까지만 가능합니다.");
        }
    }
    
    /**
     * 🆕 거래처 조회
     */
    private Company getCompany(Long companyId) {
        return clientRepo.findById(companyId)
                .orElseThrow(() -> new RuntimeException("거래처를 찾을 수 없습니다."));
    }
}