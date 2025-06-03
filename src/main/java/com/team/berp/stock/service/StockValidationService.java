package com.team.berp.stock.service;

import com.team.berp.domain.Stock;
import com.team.berp.stock.dto.StockRequestDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockValidationService {

    // 재고 요청 기본 검증
    public void validate(StockRequestDTO req) {
        if (req.getItemId() == null) {
            throw new IllegalArgumentException("품목 ID는 필수입니다.");
        }
        
        if (req.getWarehouseId() == null) {
            throw new IllegalArgumentException("창고 ID는 필수입니다.");
        }
        
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
    }

    // 재고 부족 검증
    public void checkQty(Stock stock, Integer requestQty) {
        if (stock.getQuantity() < requestQty) {
            throw new RuntimeException(
                String.format("재고 부족: 현재 %d개, 요청 %d개", 
                    stock.getQuantity(), requestQty)
            );
        }
    }

    // 재고 존재 검증
    public void checkStock(Stock stock) {
        if (stock == null) {
            throw new RuntimeException("해당 재고가 존재하지 않습니다.");
        }
    }
}