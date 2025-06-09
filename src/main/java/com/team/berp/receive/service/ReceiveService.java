package com.team.berp.receive.service;

import com.team.berp.domain.*;
import com.team.berp.inventory_log.service.InventoryLogService;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.warehouse.repository.Warehouse_repository;
import com.team.berp.client.repository.ClientRepository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import com.team.berp.place.repository.PlaceRepository;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import com.team.berp.stock.repository.StockRepository;
import com.team.berp.receive.dto.ReceiveRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiveService {

    private final ItemRepository itemRepo;
    private final Warehouse_repository warehouseRepo;
    private final ClientRepository clientRepo;
    private final Order_OrderLineItemRepository orderLineItemRepo;
    private final PlaceRepository placeRepo;
    private final InventoryLogRepository inventoryLogRepo;
    private final StockRepository stockRepo;
    private final InventoryLogService inventoryLogService;

    /**
     * 🆕 입고 처리 - 발주 상태 자동 업데이트 포함
     */
    @Transactional
    public void processReceive(ReceiveRequestDTO request) {
        // 유효성 검증
        if (!request.isValid()) {
            throw new IllegalArgumentException("입고 요청 데이터가 유효하지 않습니다.");
        }

        // 품목 조회
        Item item = itemRepo.findById(request.getItemId())
            .orElseThrow(() -> new IllegalArgumentException("품목을 찾을 수 없습니다."));

        // 창고 조회
        Warehouse warehouse = warehouseRepo.findById(request.getWarehouseId())
            .orElseThrow(() -> new IllegalArgumentException("창고를 찾을 수 없습니다."));

        // 사용 중인 창고인지 확인
        if (!"Y".equals(warehouse.getUseYn())) {
            throw new IllegalArgumentException("사용 중지된 창고입니다.");
        }

        String comment = request.generateComment();
        OrderLineItem orderLineItem = null;

        // 입고 유형별 처리
        if ("ORDER_BASED".equals(request.getReceiveType())) {
            // 발주 기반 입고
            orderLineItem = processOrderBasedReceive(request, item, comment);
        } else {
            // 독립적 입고
            comment = processIndependentReceive(request, comment);
        }

        // 재고 업데이트
        updateStock(item, warehouse, request.getQuantity());

        // 입고 로그 생성
        InventoryLog log = createReceiveLog(item, warehouse, request.getQuantity(), 
                                          comment, orderLineItem);

        // 🆕 발주 기반 입고인 경우 발주 상태 업데이트 검사
        if (orderLineItem != null) {
            checkAndUpdateOrderStatus(orderLineItem);
        }

        System.out.println("✅ 입고 처리 완료 - " + item.getCode() + " " + request.getQuantity() + "개");
    }
    

    /**
     * 발주 기반 입고 처리
     */
    private OrderLineItem processOrderBasedReceive(ReceiveRequestDTO request, Item item, String comment) {
        if (request.getOrderLineItemId() == null) {
            throw new IllegalArgumentException("발주 기반 입고에서는 발주 정보가 필요합니다.");
        }

        OrderLineItem orderLineItem = orderLineItemRepo.findById(request.getOrderLineItemId())
            .orElseThrow(() -> new IllegalArgumentException("발주 정보를 찾을 수 없습니다."));
 
        // 🆕 발주 상태 검증
        if (orderLineItem.getCompanyOrder() == null || 
            orderLineItem.getCompanyOrder().getOrderStatus() != CompanyOrder.OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("입고 처리가 불가능한 발주 상태입니다. (CONFIRMED 상태만 입고 가능)");
        }

        // 품목 일치 여부 확인
        if (!item.getId().equals(orderLineItem.getItem().getId())) {
            throw new IllegalArgumentException("선택한 품목과 발주 품목이 일치하지 않습니다.");
        }

        // 입고 가능 수량 확인
        Integer orderQty = orderLineItem.getUnitQty();
        Integer receivedQty = getReceivedQuantity(orderLineItem.getOrderLineItemId());
        Integer remainingQty = orderQty - receivedQty;

        if (request.getQuantity() > remainingQty) {
            throw new IllegalArgumentException(
                String.format("입고 가능 수량을 초과했습니다. (가능: %d개, 요청: %d개)", 
                            remainingQty, request.getQuantity()));
        }

        return orderLineItem;
    }

    /**
     * 독립적 입고 처리
     */
    private String processIndependentReceive(ReceiveRequestDTO request, String comment) {
        StringBuilder commentBuilder = new StringBuilder(comment);

        // 공급업체 정보 추가
        if (request.getSupplierId() != null) {
            Company supplier = clientRepo.findById(request.getSupplierId())
                .orElse(null);

            if (supplier != null) {
                commentBuilder.append(" 공급업체: ").append(supplier.getCompanyName());

                if (supplier.getEmployee() != null) {
                    commentBuilder.append(", 담당자: ").append(supplier.getEmployee().getEmpName());
                }
            }
        }

        return commentBuilder.toString();
    }

    /**
     * 재고 업데이트
     */
    private void updateStock(Item item, Warehouse warehouse, Integer quantity) {
        Optional<Stock> existingStock = stockRepo.findByItemAndWarehouse(item, warehouse);

        if (existingStock.isPresent()) {
            // 기존 재고 업데이트
            Stock stock = existingStock.get();
            stock.setQuantity(stock.getQuantity() + quantity);
            stockRepo.save(stock);
        } else {
            // 새로운 재고 생성
            Stock newStock = new Stock();
            newStock.setItem(item);
            newStock.setWarehouse(warehouse);
            newStock.setQuantity(quantity);
            newStock.setFirstStockedDate(LocalDateTime.now());
            newStock.setLastStockedDate(LocalDateTime.now());
            stockRepo.save(newStock);
        }
    }

    
    /**
     * 입고 로그 생성
     */
    private InventoryLog createReceiveLog(Item item, Warehouse warehouse, Integer quantity, 
                                        String comment, OrderLineItem orderLineItem) {
        InventoryLog log = InventoryLog.builder()
            .logType(LogType.IN)
            .item(item)
            .warehouse(warehouse)
            .quantity(quantity)
            .logDatetime(LocalDateTime.now())
            .comment(comment)
            .logStatus(LogStatus.CONFIRMED)
            .orderLineItem(orderLineItem)
            .build();

        return inventoryLogRepo.save(log);
    }

    /**
     * 🆕 발주 상태 업데이트 검사 및 처리
     */
    private void checkAndUpdateOrderStatus(OrderLineItem orderLineItem) {
        try {
            // 해당 발주의 모든 라인 아이템 확인
            CompanyOrder order = orderLineItem.getCompanyOrder();
            if (order == null) return;

            boolean allCompleted = order.getLineItems().stream()
                .allMatch(oli -> {
                    Integer orderQty = oli.getUnitQty();
                    Integer receivedQty = getReceivedQuantity(oli.getOrderLineItemId());
                    return orderQty <= receivedQty; // 주문 수량 <= 입고 수량
                });

            if (allCompleted) {
                // 모든 라인 아이템이 입고 완료된 경우
                order.setOrderStatus(CompanyOrder.OrderStatus.COMPLETED);
                placeRepo.save(order);
                System.out.println("🔄 발주 상태 업데이트: " + order.getOrderNum() + " → COMPLETED");
            }

        } catch (Exception e) {
            System.err.println("❌ 발주 상태 업데이트 실패: " + e.getMessage());
            // 발주 상태 업데이트 실패가 입고 처리를 방해하지 않도록 예외를 던지지 않음
        }
    }

    /**
     * 특정 발주 라인 아이템의 입고된 수량 조회
     */
    private Integer getReceivedQuantity(Long orderLineItemId) {
        return inventoryLogRepo.findAll().stream()
            .filter(log -> LogType.IN.equals(log.getLogType()) && 
                         orderLineItemId.equals(log.getOrderLineItem() != null ? 
                         log.getOrderLineItem().getOrderLineItemId() : null))
            .mapToInt(log -> log.getQuantity() != null ? log.getQuantity() : 0)
            .sum();
    }

    /**
     * 오늘 입고 요약 통계
     */
    public ReceiveSummary getTodayReceiveSummary() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);

            // 오늘 입고 로그 조회
            List<InventoryLog> todayLogs = inventoryLogRepo.findByLogTypeAndLogDatetimeBetween(
                LogType.IN, startOfDay, endOfDay);

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

    /**
     * 입고 요약 정보 레코드
     */
    public record ReceiveSummary(int totalCount, int totalQuantity) {
        public String getFormattedSummary() {
            if (totalCount == 0) {
                return "입고 없음";
            }
            return String.format("%d건 (총 %,d개)", totalCount, totalQuantity);
        }
    }
}