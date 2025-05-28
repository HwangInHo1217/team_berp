package com.team.berp.inventory_log.service;

import com.team.berp.domain.*;
import com.team.berp.inventory_log.dto.InventoryLogResponseDTO;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryLogService {
    
    private final InventoryLogRepository logRepo;
    
    // 재고 변동 로그 생성
    @Transactional
    public void createLog(LogType logType, Item item, Warehouse whs, Integer qty, String comment) {
        InventoryLog log = new InventoryLog();
        log.setLogType(logType);
        log.setItem(item);
        log.setWarehouse(whs);
        log.setQuantity(qty);
        log.setLogDatetime(LocalDateTime.now());
        log.setComment(comment);
        
        // 입고일 때만 상태 설정
        if (logType == LogType.IN) {
            log.setLogStatus(LogStatus.CONFIRMED);
        }
        
        logRepo.save(log);
    }
    
    // 재고 이력 조회
    public Page<InventoryLogResponseDTO> getStockHistory(Long itemId, Long warehouseId, Pageable pageable) {
        Page<InventoryLog> logs = logRepo.findByItemIdAndWarehouseIdOrderByLogDatetimeDesc(
            itemId, warehouseId, pageable);
        
        return logs.map(InventoryLogResponseDTO::fromEntity);
    }
    
    // 품목별 전체 이력 조회
    public Page<InventoryLogResponseDTO> getItemHistory(Long itemId, Pageable pageable) {
        Page<InventoryLog> logs = logRepo.findByItemIdOrderByLogDatetimeDesc(itemId, pageable);
        return logs.map(InventoryLogResponseDTO::fromEntity);
    }
    
    // 창고별 전체 이력 조회
    public Page<InventoryLogResponseDTO> getWarehouseHistory(Long warehouseId, Pageable pageable) {
        Page<InventoryLog> logs = logRepo.findByWarehouseIdOrderByLogDatetimeDesc(warehouseId, pageable);
        return logs.map(InventoryLogResponseDTO::fromEntity);
    }
    
    // 특정 재고의 최종 입고일 조회
    public LocalDateTime lastInDate(Long itemId, Long warehouseId) {
        return logRepo.findLastInDate(itemId, warehouseId).orElse(null);
    }
    
    // 특정 재고의 최종 출고일 조회
    public LocalDateTime lastOutDate(Long itemId, Long warehouseId) {
        return logRepo.findLastOutDate(itemId, warehouseId).orElse(null);
    }
    
    // 기간별 입출고 통계
    public StockMovementStats getMovementStats(Long itemId, Long warehouseId, 
                                               LocalDateTime startDate, LocalDateTime endDate) {
        Integer inQty = logRepo.sumInQuantityByPeriod(itemId, warehouseId, startDate, endDate)
                               .orElse(0);
        Integer outQty = logRepo.sumOutQuantityByPeriod(itemId, warehouseId, startDate, endDate)
                                .orElse(0);
        
        return new StockMovementStats(inQty, outQty);
    }
    
    // 통계 데이터 클래스
    public record StockMovementStats(Integer totalIn, Integer totalOut) {
        public Integer getNetMovement() {
            return totalIn - totalOut;
        }
    }
}