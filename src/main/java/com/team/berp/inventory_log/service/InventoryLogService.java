package com.team.berp.inventory_log.service;

import com.team.berp.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryLogService {

    // private final InventoryLogRepository logRepo; // TODO: Repository 추가 필요

    // 재고 변동 로그 생성
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
        
        // logRepo.save(log); // TODO: 실제 저장
        System.out.println("로그 생성: " + logType + ", 품목: " + item.getName() + ", 창고: " + whs.getWarehouseName() + ", 수량: " + qty);
    }

    // 특정 재고의 최종 입고일 조회
    public LocalDateTime lastInDate(Long itemId, Integer warehouseId) {
        // TODO: 로그에서 마지막 입고일 조회
        return null;
    }

    // 특정 재고의 최종 출고일 조회
    public LocalDateTime lastOutDate(Long itemId, Integer warehouseId) {
        // TODO: 로그에서 마지막 출고일 조회
        return null;
    }
}