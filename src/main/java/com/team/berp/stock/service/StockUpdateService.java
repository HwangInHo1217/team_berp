package com.team.berp.stock.service;

import com.team.berp.domain.Stock;
import com.team.berp.domain.Item;
import com.team.berp.domain.Warehouse;
import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 재고 업데이트 서비스
 * - 재고 수량 증감 및 생성 로직 처리
 * - LOT 번호 관련 기능 제거
 * - 시간 정보 자동 관리
 */
@Service
@RequiredArgsConstructor
public class StockUpdateService {
    
    private final StockRepository stockRepo;
    
    /**
     * 재고 증가
     */
    public void addQty(Stock stock, Integer qty) {
        stock.setQuantity(stock.getQuantity() + qty);
        stock.setLastStockedDate(LocalDateTime.now());
        stockRepo.save(stock);
        
        System.out.println("✅ 재고 증가: " + stock.getItem().getName() + 
                          " (" + (stock.getQuantity() - qty) + " → " + stock.getQuantity() + ")");
    }
    
    /**
     * 재고 감소
     */
    public void subQty(Stock stock, Integer qty) {
        int oldQty = stock.getQuantity();
        stock.setQuantity(stock.getQuantity() - qty);
        stock.setLastStockedDate(LocalDateTime.now());
        stockRepo.save(stock);
        
        System.out.println("✅ 재고 감소: " + stock.getItem().getName() + 
                          " (" + oldQty + " → " + stock.getQuantity() + ")");
    }
    
    /**
     * 새 재고 생성 (LOT 번호 제거)
     */
    public Stock createStock(StockRequestDTO req, Item item, Warehouse whs) {
        Stock newStock = new Stock();
        newStock.setItem(item);
        newStock.setWarehouse(whs);
        newStock.setQuantity(req.getQuantity());
        // LOT 번호 설정 제거
        newStock.setFirstStockedDate(LocalDateTime.now());
        newStock.setLastStockedDate(LocalDateTime.now());
        
        Stock savedStock = stockRepo.save(newStock);
        
        System.out.println("✅ 새 재고 생성: " + item.getName() + 
                          " (" + whs.getWarehouseName() + ", " + req.getQuantity() + "개)");
        
        return savedStock;
    }
    
    /**
     * 재고 직접 설정 (재고 조정용 - 필요시에만 사용)
     */
    public void setQty(Stock stock, Integer qty) {
        int oldQty = stock.getQuantity();
        stock.setQuantity(qty);
        stock.setLastStockedDate(LocalDateTime.now());
        stockRepo.save(stock);
        
        System.out.println("✅ 재고 조정: " + stock.getItem().getName() + 
                          " (" + oldQty + " → " + qty + ")");
    }
}