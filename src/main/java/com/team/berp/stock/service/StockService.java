package com.team.berp.stock.service;

import com.team.berp.domain.*;
import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.repository.StockRepository;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;

import lombok.RequiredArgsConstructor;

import java.util.List;

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
    
    // 재고 목록 조회 (개선된 버전)
    public Page<StockResponseDTO> getList(String keyword, String whsCode, 
                                         String itemType, String stockStatus, 
                                         Pageable page) {
        return stockBiz.searchStocks(keyword, whsCode, itemType, stockStatus, page);
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
    
    // 재고 조정
    @Transactional
    public void adjustStock(Long stockId, Integer actualQty, String reason) {
        stockBiz.adjustStock(stockId, actualQty, reason);
    }
    
    // 엑셀 생성 (추후 구현)
    public byte[] generateExcel(String keyword, String whsCode, 
                               String itemType, String stockStatus) {
        // TODO: Apache POI를 사용한 엑셀 생성
        // 1. 검색 조건으로 데이터 조회
        // 2. 엑셀 워크북 생성
        // 3. 헤더 및 데이터 행 추가
        // 4. 스타일 적용
        // 5. byte[] 반환
        return new byte[0];
    }
    
    
}