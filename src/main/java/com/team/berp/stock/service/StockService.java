package com.team.berp.stock.service;

import com.team.berp.domain.*;
import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepo;
    private final StockBusinessService stockBiz;

    // 재고 목록 조회
    public Page<StockResponseDTO> getList(String keyword, String whsCode, Pageable page) {
        return stockBiz.searchStocks(keyword, whsCode, page);
    }

    // 재고 상세 조회
    public StockResponseDTO getDetail(Long stockId) {
        return stockBiz.getStockDetail(stockId);
    }

    // 입고 처리
    @Transactional
    public void stockIn(StockRequestDTO req) {
        stockBiz.stockIn(req);
    }

    // 출고 처리
    @Transactional
    public void stockOut(StockRequestDTO req) {
        stockBiz.stockOut(req);
    }

    // 폐기 처리
    @Transactional
    public void dispose(StockRequestDTO req) {
        stockBiz.dispose(req);
    }

    // 반품입고 처리
    @Transactional
    public void returnIn(StockRequestDTO req) {
        stockBiz.returnIn(req);
    }
}