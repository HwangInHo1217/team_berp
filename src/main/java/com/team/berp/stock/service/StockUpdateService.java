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
        stock.setUpdatedAt(LocalDateTime.now());
        stockRepo.save(stock);
    }

    // 재고 감소
    public void subQty(Stock stock, Integer qty) {
        stock.setQuantity(stock.getQuantity() - qty);
        stock.setUpdatedAt(LocalDateTime.now());
        stockRepo.save(stock);
    }

    // 새 재고 생성 (Item, Warehouse 엔티티 받기)
    public Stock createStock(StockRequestDTO req, Item item, Warehouse whs) {
        Stock newStock = new Stock();
        newStock.setItem(item);
        newStock.setWhs(whs);
        newStock.setQuantity(req.getQuantity());
        newStock.setLotNum(req.getLotNum());
        newStock.setStockedAt(LocalDateTime.now());
        newStock.setUpdatedAt(LocalDateTime.now());
        
        return stockRepo.save(newStock);
    }
}