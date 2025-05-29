// ===== StockBusinessService.java =====
package com.team.berp.stock.service;

import com.team.berp.domain.*;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.repository.StockRepository;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.warehouse.repository.Warehouse_repository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 🏭 재고 비즈니스 로직 서비스
 * - 복잡한 재고 처리 로직 담당
 * - 다른 서비스들과 협력하여 업무 규칙 구현
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 🔒 기본 읽기전용 트랜잭션
public class StockBusinessService {
    
    // 🔗 의존성 주입된 서비스들
    private final StockRepository stockRepo;
    private final ItemRepository itemRepo;
    private final Warehouse_repository whsRepo;
    private final StockValidationService validationSvc;  // 유효성 검증 서비스
    private final StockUpdateService updateSvc;          // 재고 업데이트 서비스
    private final InventoryLogService logSvc;            // 이력 생성 서비스
    private final InventoryLogRepository logRepo;        // 이력 조회용 Repository
    
    /**
     * 🔍 재고 검색 로직
     * 조건별 우선순위: keyword → whsCode → itemType → stockStatus → 전체
     * 
     * @param keyword 검색 키워드 (품목명/코드, 창고명/코드 통합 검색)
     * @param whsCode 창고 코드 필터
     * @param itemType 품목 유형 필터
     * @param stockStatus 재고 상태 필터 (inStock/outOfStock)
     * @param page 페이징 정보
     * @return 검색된 재고 목록 (DTO 변환 완료)
     */
    public Page<StockResponseDTO> searchStocks(String keyword, String whsCode, String itemType, 
                                              String stockStatus, Pageable page) {
        Page<Stock> stocks;
        
        try {
            // 🔍 검색 조건별 분기 처리 (우선순위 순서)
            if (!isEmpty(keyword)) {
                // 키워드 검색 (품목명/코드, 창고명/코드 통합)
                stocks = stockRepo.searchByKeyword(keyword.trim(), page);
            } else if (!isEmpty(whsCode)) {
                // 창고 코드로 검색
                stocks = stockRepo.findByWarehouseCode(whsCode, page);
            } else if (!isEmpty(itemType)) {
                // 품목 유형으로 검색
                ItemType itemTypeEnum = parseItemType(itemType);
                stocks = stockRepo.findByItemType(itemTypeEnum, page);
            } else if (!isEmpty(stockStatus)) {
                // 재고 상태로 검색 (Java 14 switch expression 사용)
                stocks = switch (stockStatus) {
                    case "inStock" -> stockRepo.findInStock(page);      // 재고 있음
                    case "outOfStock" -> stockRepo.findOutOfStock(page); // 재고 없음
                    default -> stockRepo.findAll(page);                 // 전체
                };
            } else {
                // 조건 없음 - 전체 조회
                stocks = stockRepo.findAll(page);
            }
            
            // 🔄 Entity → DTO 변환 및 추가 정보 조회
            return stocks.map(stock -> {
                try {
                    // 기본 정보 변환
                    StockResponseDTO dto = StockResponseDTO.fromEntity(stock);
                    
                    // 📅 최종 입고일 조회 및 설정
                    try {
                        logRepo.findLastInDate(stock.getItem().getId(), stock.getWarehouse().getId())
                               .ifPresent(date -> {
                                   dto.setLastInDate(date);        // 최종 입고일
                                   dto.setActualLastInAt(date);    // 실제 최종 입고일시
                               });
                    } catch (Exception e) {
                        System.err.println("입고일 조회 오류: " + e.getMessage());
                    }
                    
                    // 📅 최종 출고일 조회 및 설정
                    try {
                        logRepo.findLastOutDate(stock.getItem().getId(), stock.getWarehouse().getId())
                               .ifPresent(date -> {
                                   dto.setLastOutDate(date);       // 최종 출고일
                                   dto.setActualLastOutAt(date);   // 실제 최종 출고일시
                               });
                    } catch (Exception e) {
                        System.err.println("출고일 조회 오류: " + e.getMessage());
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
     * 🔍 재고 상세 조회 로직
     * 특정 재고 ID의 상세 정보 + 최종 입출고일 조회
     * 
     * @param stockId 재고 ID
     * @return 재고 상세 정보 DTO
     */
    public StockResponseDTO getStockDetail(Long stockId) {
        try {
            // 📦 재고 엔티티 조회
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new RuntimeException("재고 정보를 찾을 수 없습니다."));
            
            // 🔄 기본 정보 DTO 변환
            StockResponseDTO dto = StockResponseDTO.fromEntity(stock);
            
            // 📅 추가 정보 조회 (최종 입출고일)
            try {
                // 최종 입고일 조회
                logRepo.findLastInDate(stock.getItem().getId(), stock.getWarehouse().getId())
                       .ifPresent(date -> {
                           dto.setLastInDate(date);
                           dto.setActualLastInAt(date);
                       });
                       
                // 최종 출고일 조회
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
     * 📦 입고 처리 로직
     * 유효성검증 → 품목/창고조회 → 기존재고확인 → 수량증가/신규생성 → 이력기록
     * 
     * @param req 입고 요청 DTO
     */
    @Transactional  // 🔒 쓰기 트랜잭션
    public void stockIn(StockRequestDTO req) {
        // 1️⃣ 유효성 검증 (필수값, 수량 등)
        validationSvc.validate(req);
        
        // 2️⃣ 품목, 창고 엔티티 조회
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        // 3️⃣ 기존 재고 존재 여부 확인
        Optional<Stock> stock = stockRepo.findByItemAndWarehouse(item, whs);
        
        if (stock.isPresent()) {
            // 기존 재고가 있으면 수량 증가
            updateSvc.addQty(stock.get(), req.getQuantity());
        } else {
            // 기존 재고가 없으면 신규 생성
            updateSvc.createStock(req, item, whs);
        }
        
        // 4️⃣ 입고 이력 기록
        logSvc.createLog(LogType.IN, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "입고 처리");
    }
    
    /**
     * 📤 출고 처리 로직
     * 유효성검증 → 재고조회 → 수량확인 → 수량차감 → 이력기록
     * 
     * @param req 출고 요청 DTO
     */
    @Transactional
    public void stockOut(StockRequestDTO req) {
        // 1️⃣ 유효성 검증
        validationSvc.validate(req);
        
        // 2️⃣ 품목, 창고 엔티티 조회
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        // 3️⃣ 현재 재고 조회 (없으면 예외 발생)
        Stock stock = stockRepo.findByItemAndWarehouse(item, whs)
                .orElseThrow(() -> new RuntimeException("출고할 재고가 없습니다."));
        
        // 4️⃣ 출고 가능 수량 확인 (재고 부족시 예외 발생)
        validationSvc.checkQty(stock, req.getQuantity());
        
        // 5️⃣ 재고 수량 차감
        updateSvc.subQty(stock, req.getQuantity());
        
        // 6️⃣ 출고 이력 기록
        logSvc.createLog(LogType.OUT, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "출고 처리");
    }
    
    /**
     * 🗑️ 폐기 처리 로직
     * 출고와 동일한 로직이지만 이력 유형만 DISPOSE로 구분
     * 
     * @param req 폐기 요청 DTO
     */
    @Transactional
    public void dispose(StockRequestDTO req) {
        // 1️⃣ 유효성 검증
        validationSvc.validate(req);
        
        // 2️⃣ 품목, 창고 엔티티 조회
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        // 3️⃣ 현재 재고 조회
        Stock stock = stockRepo.findByItemAndWarehouse(item, whs)
                .orElseThrow(() -> new RuntimeException("폐기할 재고가 없습니다."));
        
        // 4️⃣ 폐기 가능 수량 확인
        validationSvc.checkQty(stock, req.getQuantity());
        
        // 5️⃣ 재고 수량 차감 (출고와 동일)
        updateSvc.subQty(stock, req.getQuantity());
        
        // 6️⃣ 폐기 이력 기록 (LogType.DISPOSE로 구분)
        logSvc.createLog(LogType.DISPOSE, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "폐기 처리");
    }
    
    /**
     * ↩️ 반품입고 처리 로직
     * 입고와 동일한 로직이지만 이력 유형만 RETURN_IN으로 구분
     * 
     * @param req 반품입고 요청 DTO
     */
    @Transactional
    public void returnIn(StockRequestDTO req) {
        // 1️⃣ 유효성 검증
        validationSvc.validate(req);
        
        // 2️⃣ 품목, 창고 엔티티 조회
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        // 3️⃣ 기존 재고 존재 여부 확인
        Optional<Stock> stock = stockRepo.findByItemAndWarehouse(item, whs);
        
        if (stock.isPresent()) {
            // 기존 재고가 있으면 수량 증가
            updateSvc.addQty(stock.get(), req.getQuantity());
        } else {
            // 기존 재고가 없으면 신규 생성
            updateSvc.createStock(req, item, whs);
        }
        
        // 4️⃣ 반품입고 이력 기록 (LogType.RETURN_IN으로 구분)
        logSvc.createLog(LogType.RETURN_IN, item, whs, req.getQuantity(), 
                        req.getComment() != null ? req.getComment() : "반품입고 처리");
    }
    
    // 🛠️ === 유틸리티 메소드들 ===
    
    /**
     * 🔧 문자열 비어있음 체크
     * null 또는 공백만 있는 문자열 확인
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 🔧 품목 유형 문자열 → Enum 변환
     * 잘못된 값인 경우 null 반환 (예외 발생하지 않음)
     */
    private ItemType parseItemType(String itemTypeStr) {
        if (isEmpty(itemTypeStr)) return null;
        
        try {
            return ItemType.valueOf(itemTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("유효하지 않은 품목 유형: " + itemTypeStr);
            return null;
        }
    }
    
    /**
     * 🔧 품목 엔티티 조회
     * 존재하지 않으면 RuntimeException 발생
     */
    private Item getItem(Long itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("품목을 찾을 수 없습니다."));
    }
    
    /**
     * 🔧 창고 엔티티 조회
     * 존재하지 않으면 RuntimeException 발생
     */
    private Warehouse getWarehouse(Long warehouseId) {
        return whsRepo.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("창고를 찾을 수 없습니다."));
    }
}