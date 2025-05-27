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
    
    // 재고 검색 로직 (개선됨)
    public Page<StockResponseDTO> searchStocks(String keyword, String whsCode, String itemType, 
                                              String stockStatus, Pageable page) {
        Page<Stock> stocks;
        
        // 복합 검색 조건 처리
        if (!isEmpty(itemType) && !isEmpty(whsCode)) {
            stocks = stockRepo.findByItemTypeAndWarehouseCode(itemType, whsCode, page);
        } else if (!isEmpty(itemType)) {
            stocks = stockRepo.findByItemType(itemType, page);
        } else if (!isEmpty(stockStatus)) {
            stocks = switch (stockStatus) {
                case "inStock" -> stockRepo.findInStock(page);
                case "outOfStock" -> stockRepo.findOutOfStock(page);
                default -> stockRepo.findAll(page);
            };
        } else if (isEmpty(keyword)) {
            if (isEmpty(whsCode)) {
                stocks = stockRepo.findAll(page);
            } else {
                stocks = stockRepo.findByWarehouseCode(whsCode, page);
            }
        } else {
            stocks = stockRepo.searchByKeyword(keyword.trim(), page);
        }
        
        // DTO 변환 시 추가 정보 포함
        return stocks.map(stock -> {
            StockResponseDTO dto = StockResponseDTO.fromEntity(stock);
            
            // 최종 입/출고일 조회
            logRepo.findLastInDate(stock.getItem().getId(), stock.getWarehouse().getId())
                   .ifPresent(dto::setLastInDate);
            logRepo.findLastOutDate(stock.getItem().getId(), stock.getWarehouse().getId())
                   .ifPresent(dto::setLastOutDate);
            
            return dto;
        });
    }
    
    // 재고 상세 조회 로직
    public StockResponseDTO getStockDetail(Long stockId) {
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new RuntimeException("재고 정보를 찾을 수 없습니다."));
        
        StockResponseDTO dto = StockResponseDTO.fromEntity(stock);
        
        // 추가 정보 조회
        logRepo.findLastInDate(stock.getItem().getId(), stock.getWarehouse().getId())
               .ifPresent(dto::setLastInDate);
        logRepo.findLastOutDate(stock.getItem().getId(), stock.getWarehouse().getId())
               .ifPresent(dto::setLastOutDate);
        
        return dto;
    }
    
    // 입고 처리 로직
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
    
    // 출고 처리 로직
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
    
    // 폐기 처리 로직
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
    
    // 반품입고 처리 로직
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
    
    // 재고 조정 (실사)
    @Transactional
    public void adjustStock(Long stockId, Integer actualQty, String reason) {
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new RuntimeException("재고를 찾을 수 없습니다."));
        
        Integer difference = actualQty - stock.getQuantity();
        
        if (difference != 0) {
            stock.setQuantity(actualQty);
            stockRepo.save(stock);
            
            LogType logType = difference > 0 ? LogType.IN : LogType.OUT;
            logSvc.createLog(logType, stock.getItem(), stock.getWarehouse(), 
                           Math.abs(difference), "재고조정: " + reason);
        }
    }
    
    // 유틸리티 메소드들
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
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