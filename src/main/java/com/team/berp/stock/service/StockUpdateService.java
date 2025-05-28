package com.team.berp.stock.service;

import com.team.berp.domain.Stock;
import com.team.berp.domain.Item;
import com.team.berp.domain.Warehouse;
import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StockUpdateService {
    
    private final StockRepository stockRepo;
    
    // 재고 증가
    public void addQty(Stock stock, Integer qty) {
        stock.setQuantity(stock.getQuantity() + qty);
        stock.setLastStockedDate(LocalDateTime.now());
        stockRepo.save(stock);
    }
    
    // 재고 감소
    public void subQty(Stock stock, Integer qty) {
        stock.setQuantity(stock.getQuantity() - qty);
        stock.setLastStockedDate(LocalDateTime.now());
        stockRepo.save(stock);
    }
    
    // 새 재고 생성
    public Stock createStock(StockRequestDTO req, Item item, Warehouse whs) {
        Stock newStock = new Stock();
        newStock.setItem(item);
        newStock.setWarehouse(whs);
        newStock.setQuantity(req.getQuantity());
        newStock.setLotNumber(req.getLotNumber());
        newStock.setFirstStockedDate(LocalDateTime.now());
        newStock.setLastStockedDate(LocalDateTime.now());
        
        return stockRepo.save(newStock);
    }
    
    // 재고 직접 설정 (재고 조정용)
    public void setQty(Stock stock, Integer qty) {
        stock.setQuantity(qty);
        stock.setLastStockedDate(LocalDateTime.now());
        stockRepo.save(stock);
    }
    
    // LOT 번호 업데이트
    public void updateLotNumber(Stock stock, String lotNumber) {
        stock.setLotNumber(lotNumber);
        stock.setLastStockedDate(LocalDateTime.now());
        stockRepo.save(stock);
    }
}