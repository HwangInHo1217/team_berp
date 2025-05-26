package com.team.berp.stock.service;

import com.team.berp.domain.*;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockBusinessService {

    private final StockRepository stockRepo;
    private final ItemRepository itemRepo;
    private final Warehouse_repository whsRepo;
    private final StockValidationService validationSvc;
    private final StockUpdateService updateSvc;
    private final InventoryLogService logSvc;

    // 재고 검색 로직
    public Page<StockResponseDTO> searchStocks(String keyword, String whsCode, Pageable page) {
        Page<Stock> stocks;
        
        if (isEmpty(keyword)) {
            if (isEmpty(whsCode) || "전체 창고".equals(whsCode)) {
                stocks = stockRepo.findAll(page);
            } else {
                stocks = stockRepo.findByWhsName(whsCode, page);
            }
        } else {
            stocks = stockRepo.searchByKeyword(keyword.trim(), page);
        }
        
        return stocks.map(StockResponseDTO::fromEntity);
    }

    // 재고 상세 조회 로직
    public StockResponseDTO getStockDetail(Long stockId) {
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new RuntimeException("재고 정보를 찾을 수 없습니다."));
        return StockResponseDTO.fromEntity(stock);
    }

    // 입고 처리 로직
    public void stockIn(StockRequestDTO req) {
        validationSvc.validate(req);
        
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        Optional<Stock> stock = stockRepo.findByItemAndWhs(item, whs);
        
        if (stock.isPresent()) {
            updateSvc.addQty(stock.get(), req.getQuantity());
        } else {
            updateSvc.createStock(req, item, whs);
        }
        
        logSvc.createLog(LogType.IN, item, whs, req.getQuantity(), "입고 처리");
    }

    // 출고 처리 로직
    public void stockOut(StockRequestDTO req) {
        validationSvc.validate(req);
        
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        Stock stock = stockRepo.findByItemAndWhs(item, whs)
                .orElseThrow(() -> new RuntimeException("출고할 재고가 없습니다."));
        
        validationSvc.checkQty(stock, req.getQuantity());
        updateSvc.subQty(stock, req.getQuantity());
        logSvc.createLog(LogType.OUT, item, whs, req.getQuantity(), "출고 처리");
    }

    // 폐기 처리 로직
    public void dispose(StockRequestDTO req) {
        validationSvc.validate(req);
        
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        Stock stock = stockRepo.findByItemAndWhs(item, whs)
                .orElseThrow(() -> new RuntimeException("폐기할 재고가 없습니다."));
        
        validationSvc.checkQty(stock, req.getQuantity());
        updateSvc.subQty(stock, req.getQuantity());
        logSvc.createLog(LogType.DISPOSE, item, whs, req.getQuantity(), "폐기 처리");
    }

    // 반품입고 처리 로직
    public void returnIn(StockRequestDTO req) {
        validationSvc.validate(req);
        
        Item item = getItem(req.getItemId());
        Warehouse whs = getWarehouse(req.getWarehouseId());
        
        Optional<Stock> stock = stockRepo.findByItemAndWhs(item, whs);
        
        if (stock.isPresent()) {
            updateSvc.addQty(stock.get(), req.getQuantity());
        } else {
            updateSvc.createStock(req, item, whs);
        }
        
        logSvc.createLog(LogType.RETURN_IN, item, whs, req.getQuantity(), "반품입고 처리");
    }

    // 유틸리티 메소드들
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private Item getItem(Long itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("품목을 찾을 수 없습니다."));
    }

    private Warehouse getWarehouse(Integer warehouseId) {
        return whsRepo.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("창고를 찾을 수 없습니다."));
    }
}