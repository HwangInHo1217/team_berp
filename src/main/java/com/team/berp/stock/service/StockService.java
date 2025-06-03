package com.team.berp.stock.service;

import java.util.HashMap; 
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
    
    // ===== 폐기 및 반품입고 메서드 제거됨 =====
    // dispose(), returnIn() 메서드 삭제
    
    /**
     * 📊 엑셀 생성 (TODO)
     */
    public byte[] generateExcel(String keyword, String whsCode, 
                               String itemType, String stockStatus) {
        return new byte[0]; // TODO: Apache POI 구현
    }
    
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
    
    /**
     * 특정 창고의 재고 요약 통계
     * @param warehouseId 창고 ID
     * @return 창고별 재고 통계 정보
     */
    public Map<String, Object> getWarehouseStockSummary(Long warehouseId) {
        try {
            System.out.println("📊 창고별 재고 요약 통계 조회 - warehouseId: " + warehouseId);
            
            Map<String, Object> summary = new HashMap<>();
            
            // 해당 창고의 총 재고 품목 수
            long totalItems = stockRepo.countByWarehouse_Id(warehouseId);
            
            // 해당 창고의 재고 없는 품목 수
            long outOfStockItems = stockRepo.countByWarehouse_IdAndQuantity(warehouseId, 0);
            
            // 해당 창고의 안전재고 미달 품목 수 (1~9개)
            long belowSafetyItems = stockRepo.countByWarehouse_IdAndQuantityBetween(warehouseId, 1, 9);
            
            // 해당 창고의 정상 재고 품목 수
            long normalStockItems = Math.max(0, totalItems - outOfStockItems - belowSafetyItems);
            
            // 해당 창고의 총 재고량
            Long totalQuantity = stockRepo.sumQuantityByWarehouse_Id(warehouseId);
            if (totalQuantity == null) totalQuantity = 0L;
            
            summary.put("totalItems", totalItems);
            summary.put("outOfStock", outOfStockItems);
            summary.put("belowSafety", belowSafetyItems);
            summary.put("normalStock", normalStockItems);
            summary.put("totalQuantity", totalQuantity);
            
            System.out.println("✅ 창고별 재고 요약: " + summary);
            return summary;
            
        } catch (Exception e) {
            System.err.println("❌ 창고별 재고 요약 통계 조회 실패: " + e.getMessage());
            e.printStackTrace();
            
            // 실패시 기본값 반환
            Map<String, Object> defaultSummary = new HashMap<>();
            defaultSummary.put("totalItems", 0L);
            defaultSummary.put("outOfStock", 0L);
            defaultSummary.put("belowSafety", 0L);
            defaultSummary.put("normalStock", 0L);
            defaultSummary.put("totalQuantity", 0L);
            
            return defaultSummary;
        }
    }
    
    
    
    
}