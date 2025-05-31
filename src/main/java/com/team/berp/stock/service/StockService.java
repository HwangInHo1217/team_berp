package com.team.berp.stock.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.dto.StockTransferRequestDTO;
import com.team.berp.stock.repository.StockRepository;

import lombok.RequiredArgsConstructor;

//=============================================================================
//📈 StockService.java - 재고 서비스 (트랜잭션 관리)
//=============================================================================

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본 읽기전용
public class StockService {
 
 private final StockRepository stockRepo;
 private final StockBusinessService stockBiz;
 
 /**
  * 📋 재고 목록 조회 (읽기전용)
  */
 public Page<StockResponseDTO> getList(String keyword, String whsCode, 
                                      String itemType, String stockStatus, 
                                      Pageable page) {
     return stockBiz.searchStocks(keyword, whsCode, itemType, stockStatus, page);
 }
 
 /**
  * 🔍 재고 상세 조회
  */
 public StockResponseDTO getDetail(Long stockId) {
     return stockBiz.getStockDetail(stockId);
 }
 
 /**
  * 📦 입고 처리 (쓰기 트랜잭션)
  */
 @Transactional
 public void stockIn(StockRequestDTO req) {
     stockBiz.stockIn(req);
 }
 
 /**
  * 📤 출고 처리 (쓰기 트랜잭션)
  */
 @Transactional
 public void stockOut(StockRequestDTO req) {
     stockBiz.stockOut(req);
 }
 
 /**
  * 🗑️ 폐기 처리
  */
 @Transactional
 public void dispose(StockRequestDTO req) {
     stockBiz.dispose(req);
 }
 
 /**
  * ↩️ 반품 입고
  */
 @Transactional
 public void returnIn(StockRequestDTO req) {
     stockBiz.returnIn(req);
 }
 
 /**
  * 📊 엑셀 생성 (TODO)
  */
 public byte[] generateExcel(String keyword, String whsCode, 
                            String itemType, String stockStatus) {
     return new byte[0]; // TODO: Apache POI 구현
 }
 
//===== StockService.java에 추가할 메서드들 =====

/**
* 🔄 창고간 재고 이동 (트랜잭션 관리)
*/
@Transactional
public void transferStock(StockTransferRequestDTO req) {
  stockBiz.transferStock(req);
}

/**
* 📊 재고 현황 요약 통계 조회
*/
public Map<String, Object> getStockSummary() {
  return stockBiz.getStockSummary();
}
 
 
 
}
