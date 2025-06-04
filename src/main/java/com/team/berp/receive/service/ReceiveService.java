package com.team.berp.receive.service;

import com.team.berp.domain.*;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.receive.dto.ReceiveRequestDTO;
import com.team.berp.receive.dto.ReceiveResponseDTO;
import com.team.berp.stock.service.StockBusinessService;
import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.warehouse.repository.Warehouse_repository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 입고 관리 서비스 - 발주기반/독립적 입고 구분 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiveService {

    private final InventoryLogRepository inventoryLogRepo;
    private final InventoryLogService inventoryLogService;
    private final StockBusinessService stockBusinessService;
    private final ItemRepository itemRepo;
    private final Warehouse_repository warehouseRepo;
    private final Order_OrderLineItemRepository orderLineItemRepo;

    /**
     * 입고 이력 조회 - 날짜 범위와 타입 필터 적용
     */
    public Page<ReceiveResponseDTO> getReceiveHistoryWithFilter(LocalDate startDate, LocalDate endDate, 
                                                              String type, Pageable pageable) {
        System.out.println("📦 입고 이력 조회 - 시작일: " + startDate + ", 종료일: " + endDate + ", 타입: " + type);
        
        try {
            Page<InventoryLog> logPage;
            
            if (startDate != null || endDate != null) {
                // 날짜 범위 설정
                LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : LocalDateTime.of(2020, 1, 1, 0, 0);
                LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
                
                logPage = inventoryLogRepo.findByLogTypeAndLogDatetimeBetweenOrderByLogDatetimeDesc(
                    LogType.IN, startDateTime, endDateTime, pageable);
            } else {
                // 전체 입고 로그 조회
                logPage = inventoryLogRepo.findByLogTypeOrderByLogDatetimeDesc(LogType.IN, pageable);
            }
            
            // InventoryLog -> ReceiveResponseDTO 변환 (타입 필터 적용)
            Page<ReceiveResponseDTO> result = logPage.map(log -> {
                ReceiveResponseDTO dto = ReceiveResponseDTO.fromInventoryLog(log);
                
                // 입고 유형 판별
                if (log.getOrderLineItem() != null) {
                    dto.setReceiveType("ORDER_BASED"); // 발주 기반
                    dto.setOrderNumber(log.getOrderLineItem().getCompanyOrder().getOrderNum());
                } else {
                    dto.setReceiveType("INDEPENDENT"); // 독립적 입고
                }
                
                return dto;
            });
            
            // 타입 필터링이 있으면 추가 필터링
            if (type != null && !type.isEmpty() && !"ALL".equals(type)) {
                // 클라이언트 측에서 필터링하거나, 별도 쿼리 메서드 작성 필요
                // 여기서는 우선 전체 결과 반환
            }
            
            System.out.println("✅ 입고 이력 조회 완료 - " + result.getTotalElements() + "건");
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 입고 이력 조회 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("입고 이력 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 입고 등록 처리 - 발주기반/독립적 입고 구분
     */
    @Transactional
    public void processReceive(ReceiveRequestDTO request) {
        System.out.println("📦 입고 등록 시작: " + request);
        
        try {
            // 1. 유효성 검증
            validateReceiveRequest(request);
            
            // 2. 필요한 엔티티 조회
            Item item = getItem(request.getItemId());
            Warehouse warehouse = getWarehouse(request.getWarehouseId());
            
            // 3. 재고 입고 처리 (기존 StockBusinessService 활용)
            StockRequestDTO stockRequest = new StockRequestDTO();
            stockRequest.setItemId(request.getItemId());
            stockRequest.setWarehouseId(request.getWarehouseId());
            stockRequest.setQuantity(request.getQuantity());
            stockRequest.setComment(request.generateComment());
            
            stockBusinessService.stockIn(stockRequest);
            
            // 4. 발주기반 입고인 경우 inventory_log에 order_line_item_id 추가
            if ("ORDER_BASED".equals(request.getReceiveType()) && request.getOrderLineItemId() != null) {
                updateInventoryLogWithOrderLineItem(item, warehouse, request);
            }
            
            System.out.println("✅ 입고 등록 완료: " + item.getName() + " " + request.getQuantity() + item.getUnit());
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 입고 등록 유효성 오류: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ 입고 등록 시스템 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("입고 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 발주기반 입고시 inventory_log에 order_line_item_id 업데이트
     */
    @Transactional
    public void updateInventoryLogWithOrderLineItem(Item item, Warehouse warehouse, ReceiveRequestDTO request) {
        try {
            // 최근 생성된 입고 로그 찾기
            List<InventoryLog> recentLogs = inventoryLogRepo.findByItemIdAndWarehouseIdOrderByLogDatetimeDesc(
                item.getId(), warehouse.getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent();
            
            if (!recentLogs.isEmpty()) {
                InventoryLog log = recentLogs.get(0);
                
                // OrderLineItem 조회 및 연결
                OrderLineItem orderLineItem = orderLineItemRepo.findById(request.getOrderLineItemId())
                    .orElseThrow(() -> new IllegalArgumentException("발주 라인 아이템을 찾을 수 없습니다."));
                
                log.setOrderLineItem(orderLineItem);
                inventoryLogRepo.save(log);
                
                System.out.println("✅ 발주기반 입고 로그 업데이트 완료 - orderLineItemId: " + request.getOrderLineItemId());
            }
            
        } catch (Exception e) {
            System.err.println("❌ 발주기반 입고 로그 업데이트 실패: " + e.getMessage());
            // 로그 업데이트 실패해도 입고는 완료된 상태이므로 예외 발생하지 않음
        }
    }

    /**
     * 특정 입고 상세 정보 조회
     */
    public ReceiveResponseDTO getReceiveDetail(Long logId) {
        System.out.println("🔍 입고 상세 조회 - logId: " + logId);
        
        try {
            InventoryLog log = inventoryLogRepo.findById(logId)
                    .orElseThrow(() -> new RuntimeException("입고 정보를 찾을 수 없습니다."));
            
            if (log.getLogType() != LogType.IN) {
                throw new RuntimeException("입고 정보가 아닙니다.");
            }
            
            ReceiveResponseDTO result = ReceiveResponseDTO.fromInventoryLog(log);
            
            // 입고 유형 및 발주 정보 설정
            if (log.getOrderLineItem() != null) {
                result.setReceiveType("ORDER_BASED");
                result.setOrderNumber(log.getOrderLineItem().getCompanyOrder().getOrderNum());
                result.setCompanyName(log.getOrderLineItem().getCompanyOrder().getCompany().getCompanyName());
            } else {
                result.setReceiveType("INDEPENDENT");
            }
            
            System.out.println("✅ 입고 상세 조회 완료: " + result.getItemName());
            return result;
            
        } catch (RuntimeException e) {
            System.err.println("❌ 입고 상세 조회 실패: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ 입고 상세 조회 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("입고 상세 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 오늘 입고 요약 통계
     */
    public ReceiveSummary getTodayReceiveSummary() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);
            
            List<InventoryLog> todayLogs = inventoryLogRepo
                .findByLogTypeAndLogDatetimeBetween(LogType.IN, startOfDay, endOfDay);
            
            int totalCount = todayLogs.size();
            int totalQuantity = todayLogs.stream()
                    .mapToInt(log -> log.getQuantity() != null ? log.getQuantity() : 0)
                    .sum();
            
            return new ReceiveSummary(totalCount, totalQuantity);
            
        } catch (Exception e) {
            System.err.println("❌ 오늘 입고 요약 조회 실패: " + e.getMessage());
            return new ReceiveSummary(0, 0);
        }
    }

    // === 내부 헬퍼 메서드들 ===

    /**
     * 입고 요청 유효성 검증
     */
    private void validateReceiveRequest(ReceiveRequestDTO request) {
        if (!request.isValid()) {
            throw new IllegalArgumentException("필수 입력값이 누락되었습니다.");
        }
        
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("입고 수량은 0보다 커야 합니다.");
        }
        
        if (request.getQuantity() > 10000) {
            throw new IllegalArgumentException("입고 수량이 너무 큽니다. (최대 10,000개)");
        }
        
        // 발주기반 입고인 경우 orderLineItemId 필수
        if ("ORDER_BASED".equals(request.getReceiveType()) && request.getOrderLineItemId() == null) {
            throw new IllegalArgumentException("발주기반 입고시 발주 라인 아이템 ID는 필수입니다.");
        }
    }

    /**
     * 품목 조회
     */
    private Item getItem(Long itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("품목을 찾을 수 없습니다."));
    }

    /**
     * 창고 조회
     */
    private Warehouse getWarehouse(Long warehouseId) {
        return warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("창고를 찾을 수 없습니다."));
    }

    /**
     * 입고 요약 통계 클래스
     */
    public record ReceiveSummary(int totalCount, int totalQuantity) {
        public String getFormattedSummary() {
            return String.format("총 %d건, %,d개", totalCount, totalQuantity);
        }
    }
}