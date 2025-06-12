// File: src/main/java/com/team/berp/order/service/OrderService.java
package com.team.berp.order.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;      // Optional 추가
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.domain.Company;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.CompanyOrder.OrderType;
import com.team.berp.domain.Item;
import com.team.berp.domain.LogStatus;
import com.team.berp.domain.LogType;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.domain.Stock;
import com.team.berp.domain.Warehouse;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import com.team.berp.mrp.service.MrpService;
import com.team.berp.order.dto.CreateOrderRequest;
import com.team.berp.order.dto.ItemWarehouseResponse;
import com.team.berp.order.dto.OrderDetailResponse;
import com.team.berp.order.dto.OrderItemRequest;
import com.team.berp.order.dto.OrderSummaryDto;
import com.team.berp.order.repository.Order_CompanyOrderRepository;
import com.team.berp.order.repository.Order_CompanyRepository;
import com.team.berp.order.repository.Order_InventoryLogRepositroy;
import com.team.berp.order.repository.Order_ItemRepository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import com.team.berp.order.repository.Order_StockRepository;
import com.team.berp.order.repository.Order_WarehouseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final Order_CompanyOrderRepository companyOrderRepository;
    private final Order_OrderLineItemRepository orderLineItemRepository;
    private final Order_CompanyRepository companyRepository;
    private final Order_ItemRepository itemRepository;
    private final Order_WarehouseRepository warehouseRepo;
    private final InventoryLogRepository inventoryLogRepository;
    private final Order_StockRepository stockRepository;
    private final Order_InventoryLogRepositroy order_InventoryLogRepositroy;

    // ★ 추가: MrpService 주입
    private final MrpService mrpService;

    // ✅ 주문번호 생성 로직: 날짜 + UUID 앞 4자리
    public String generateOrderNum() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4);
        String orderNum = "ORD-" + datePart + "-" + randomPart;
        log.debug("[generateOrderNum] Generated order number = {}", orderNum);
        return orderNum;
    }

    /**
     * 주문 등록 처리 (주문 헤더 + 주문 상세)
     */
    @Transactional
    public void createOrder(CreateOrderRequest request) {
        log.debug("[createOrder] Start processing CreateOrderRequest: {}", request);

        Integer totalQty = 0;
        long totalAmount = 0;

        // 🔹 전체 수량 및 총액 계산
        for (OrderItemRequest itemReq : request.getItems()) {
            totalQty += itemReq.getUnitQty();
            totalAmount += (long) itemReq.getUnitQty() * itemReq.getUnitPrice();
            log.debug("[createOrder] Accumulating totals: itemId={}, unitQty={}, unitPrice={}, runningTotalQty={}, runningTotalAmount={}",
                    itemReq.getItemId(), itemReq.getUnitQty(), itemReq.getUnitPrice(), totalQty, totalAmount);
        }

        // 🔹 고객사 정보 조회 및 예외 처리
        Company company = companyRepository.findById(request.getCompanyId())
            .orElseThrow(() -> {
                log.error("[createOrder] Company not found: companyId={}", request.getCompanyId());
                return new IllegalArgumentException("존재하지 않는 고객사입니다.");
            });
        log.debug("[createOrder] Found Company: companyId={}, companyName={}", company.getCompanyId(), company.getCompanyName());

        // 🔹 주문 엔티티 생성
        CompanyOrder order = CompanyOrder.builder()
            .company(company)
            .orderDate(request.getOrderDate())
            .orderType(OrderType.valueOf(request.getOrderType()))
            .note(request.getRemark())
            .orderQty(totalQty)
            .amount(totalAmount)
            .orderNum(generateOrderNum())
            .build();
        log.debug("[createOrder] Building CompanyOrder entity: {}", order);

        // 🔹 주문 상세 목록 구성
        List<OrderLineItem> lineItems = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            // 품목 ID 기준으로 품목 조회
            Item item = itemRepository.findById(itemReq.getItemId())
                .orElseThrow(() -> {
                    log.error("[createOrder] Item not found: itemId={}", itemReq.getItemId());
                    return new IllegalArgumentException("존재하지 않는 품목입니다.");
                });
            log.debug("[createOrder] Found Item: itemId={}, itemName={}", item.getId(), item.getName());

            // 주문 상세 엔티티 구성
            OrderLineItem lineItem = OrderLineItem.builder()
                .item(item)
                .unit(itemReq.getUnit())
                .unitPrice(itemReq.getUnitPrice())
                .unitQty(itemReq.getUnitQty())
                .unitPriceall(itemReq.getUnitQty() * itemReq.getUnitPrice())
                .warehouse(null) // 🔸 아직 창고는 지정하지 않음
                .build();

            // 🔹 연관관계 설정 (order ↔ lineItem)
            lineItem.setCompanyOrder(order);

            log.debug("[createOrder] Created OrderLineItem: {}", lineItem);
            lineItems.add(lineItem);
        }

        // 🔹 양방향 연관관계 설정 (order.setLineItems)
        order.setLineItems(lineItems);

        // 🔹 저장 (cascade 설정되어 있으면 lineItems도 함께 저장됨)
        companyOrderRepository.save(order);
        log.debug("[createOrder] Saved CompanyOrder (with cascading line items): orderId={}", order.getOrderId());

        companyOrderRepository.flush();
        log.debug("[createOrder] Flushed CompanyOrder repository");

        for (OrderLineItem oli : lineItems) {
            log.debug(">>> [OrderService] flush 이후 OrderLineItem ID = {}", oli.getOrderLineItemId());
        }

        // ───────────────────────────────────────────────────────────────────
        // 6) 주문 저장 직후 MRP 생성 로직 호출
        for (OrderLineItem oli : lineItems) {
            Item product = oli.getItem();
            int orderQty = Optional.ofNullable(oli.getUnitQty()).orElse(0);
            log.debug("[createOrder] Processing MRP generation for OrderLineItem: orderLineItemId={}, orderQty={}",
                    oli.getOrderLineItemId(), orderQty);

            // (2) 해당 품목(product)의 재고 리스트를 조회
            List<Stock> stocks = stockRepository.findByItemIdAndQuantityGreaterThan(
                product.getId(), 0
            );
            log.debug("[createOrder] Retrieved stocks for itemId={} (stocksSize={})", product.getId(), stocks.size());

            // (3) 총 재고 수량 합산
            int totalStockQty = stocks.stream()
                .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                .sum();
            log.debug("[createOrder] Total stock quantity for itemId={} = {}", product.getId(), totalStockQty);

            // (4) 재고가 주문수량(orderQty)보다 많거나 같으면 → 전량 출고 처리
            if (totalStockQty >= orderQty) {
                log.debug("[createOrder] Stock is sufficient (totalStockQty >= orderQty), skipping MRP generation for orderLineItemId={}", oli.getOrderLineItemId());
                // (A) “부족이 없으면 MRP 생성은 하지 않는다”라는 의미로 continue 처리
                continue;
            }

            // (5) 재고가 부족한 경우 → 부족 수량만 MRP로 넘기기
            int needQty = orderQty - totalStockQty;
            log.debug("[createOrder] Stock is insufficient, needQty={} for orderLineItemId={}", needQty, oli.getOrderLineItemId());

            // 이 바로 아래에서 “부족분만큼 MRP를 생성하는 메서드”를 호출
            log.debug(">>> [OrderService] generateMrpForOrderLineItem 호출 예정: orderLineItemId={}, needQty={}",
                    oli.getOrderLineItemId(), needQty);

            mrpService.generateMrpForOrderLineItem(oli.getOrderLineItemId(), needQty);
        }

        log.debug("[createOrder] Finished processing CreateOrderRequest: orderId={}", order.getOrderId());
    }

    // ✅ 단건 주문 조회 (상세보기용)
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId) {
        log.debug("[getOrderDetail] Fetching details for orderId={}", orderId);
        return companyOrderRepository.findById(orderId)
            .map(this::toDto)
            .orElseThrow(() -> {
                log.error("[getOrderDetail] Order not found: orderId={}", orderId);
                return new IllegalArgumentException("주문을 찾을 수 없습니다.");
            });
    }

    // ✅ 주문 도메인을 DTO로 변환 (헤더 + 상세 포함)
    private OrderDetailResponse toDto(CompanyOrder co) {
        log.debug("[toDto] Converting CompanyOrder to OrderDetailResponse: orderId={}", co.getOrderId());
        OrderDetailResponse dto = new OrderDetailResponse();
        dto.setOrderId(co.getOrderId());
        dto.setOrderNum(co.getOrderNum());
        dto.setOrderType(co.getOrderType().name());
        dto.setOrderDate(co.getOrderDate());
        dto.setOrderQty(co.getOrderQty());
        dto.setRemark(co.getNote());
        dto.setAmount(co.getAmount());
        dto.setCompanyId(co.getCompany().getCompanyId());
        dto.setCompanyName(co.getCompany().getCompanyName());
        dto.setCompanyEmpName(co.getCompany().getCompanyEmpName());
        dto.setEmpName(co.getCompany().getEmployee().getEmpName());;

        // 🔹 주문 상세 DTO 리스트 세팅
        List<OrderDetailResponse.LineItem> lineItemDtos = co.getLineItems().stream()
            .map(this::toLineItemDto)
            .collect(Collectors.toList());
        dto.setItems(lineItemDtos);
        log.debug("[toDto] Converted line items count = {}", lineItemDtos.size());

        return dto;
    }

    // ✅ 주문 상세(OrderLineItem)를 DTO로 변환
    private OrderDetailResponse.LineItem toLineItemDto(OrderLineItem oli) {
        log.debug("[toLineItemDto] Converting OrderLineItem to DTO: orderLineItemId={}", oli.getOrderLineItemId());
        OrderDetailResponse.LineItem li = new OrderDetailResponse.LineItem();
        li.setOrderLineItemId(oli.getOrderLineItemId());
        li.setItemId(oli.getItem().getId());
        li.setItemName(oli.getItem().getName());
        li.setUnit(oli.getItem().getUnit());
        li.setUnitQty(oli.getUnitQty());
        li.setUnitPrice(oli.getUnitPrice());
        li.setUnitPriceAll(oli.getUnitPriceall());
        li.setWarehouseId(oli.getWarehouse() != null ? oli.getWarehouse().getId() : null);

        // 추가: inventory_log에 ‘OUT + CONFIRMED’ 로그가 있는지 체크
        boolean shipped = order_InventoryLogRepositroy
            .existsByOrderLineItem_OrderLineItemIdAndLogTypeAndLogStatus(
                oli.getOrderLineItemId(),
                LogType.OUT,
                LogStatus.CONFIRMED
            );
        li.setAlreadyShipped(shipped);
        log.debug("[toLineItemDto] orderLineItemId={}, alreadyShipped={}", oli.getOrderLineItemId(), shipped);

        return li;
    }

    // ✅ 여러 주문 삭제
    @Transactional
    public void deleteOrders(List<Long> orderIds) {
        log.debug("[deleteOrders] Deleting orders: {}", orderIds);
        List<OrderLineItem> lineItems = orderLineItemRepository.findByCompanyOrderOrderIdIn(orderIds);
        log.debug("[deleteOrders] Fetched {} line items for deletion check", lineItems.size());

        List<OrderLineItem> deletableLineItems = new ArrayList<>();
        List<Long> blockedOrderIds = new ArrayList<>();

        for (OrderLineItem lineItem : lineItems) {
            boolean exists = inventoryLogRepository
                .existsByOrderLineItem_OrderLineItemId(lineItem.getOrderLineItemId());
            if (!exists) {
                deletableLineItems.add(lineItem);
                log.debug("[deleteOrders] LineItem deletable: orderLineItemId={}", lineItem.getOrderLineItemId());
            } else {
                blockedOrderIds.add(lineItem.getCompanyOrder().getOrderId());
                log.debug("[deleteOrders] LineItem blocked (inventory logs exist): orderLineItemId={}, orderId={}",
                        lineItem.getOrderLineItemId(), lineItem.getCompanyOrder().getOrderId());
            }
        }

        List<Long> deletableLineItemIds = deletableLineItems.stream()
            .map(OrderLineItem::getOrderLineItemId)
            .collect(Collectors.toList());
        log.debug("[deleteOrders] Deletable line item IDs: {}", deletableLineItemIds);

        if (!deletableLineItemIds.isEmpty()) {
            orderLineItemRepository.deleteAllByIdInBatch(deletableLineItemIds);
            log.debug("[deleteOrders] Deleted {} OrderLineItems in batch", deletableLineItemIds.size());
        }

        List<Long> deletableOrderIds = orderIds.stream()
            .filter(id -> !blockedOrderIds.contains(id))
            .collect(Collectors.toList());
        log.debug("[deleteOrders] Deletable order IDs: {}", deletableOrderIds);

        if (!deletableOrderIds.isEmpty()) {
            companyOrderRepository.deleteByOrderIdIn(deletableOrderIds);
            log.debug("[deleteOrders] Deleted {} CompanyOrders in batch", deletableOrderIds.size());
        }

        // ❗차단된 주문 존재 시 에러 발생
        if (!blockedOrderIds.isEmpty()) {
            log.error("[deleteOrders] Cannot delete orders with inventory logs: {}", blockedOrderIds);
            throw new RuntimeException(
                "삭제 불가: 출고/입고 기록이 있는 주문이 포함되어 있습니다. (orderIds: " + blockedOrderIds + ")"
            );
        }

        log.debug("[deleteOrders] Completed deletion of orders: {}", orderIds);
    }

    // ✅ 주문 목록 검색 + 페이징 처리 (조건: 고객사, 품목, 날짜)
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> findOrderSummaries(
            Long companyId,
            Long itemId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        log.debug("[findOrderSummaries] Searching orders with filters: companyId={}, itemId={}, fromDate={}, toDate={}, pageable={}",
                companyId, itemId, fromDate, toDate, pageable);
        // 1) 기본 OrderSummaryDto 정보만 내려주는 쿼리 수행
        Page<OrderSummaryDto> page = companyOrderRepository.findSummariesByFilters(
            companyId, itemId, fromDate, toDate, pageable
        );
        log.debug("[findOrderSummaries] Retrieved {} summaries (totalElements={})",
                page.getContent().size(), page.getTotalElements());

        // 2) 각 OrderSummaryDto에 allShipped 여부 계산
        page.forEach(dto -> {
            Long orderId = dto.getOrderId();
            log.debug("[findOrderSummaries] Checking shipment status for orderId={}", orderId);

            // 2-1) 해당 주문에 속한 모든 OrderLineItem 조회
            List<OrderLineItem> lineItems = orderLineItemRepository
                .findByCompanyOrder_OrderId(orderId);
            log.debug("[findOrderSummaries] Found {} line items for orderId={}", lineItems.size(), orderId);

            // 2-2) 하나라도 “OUT+CONFIRMED” 로그가 없으면 allShipped = false
            boolean allShipped = true;
            for (OrderLineItem oli : lineItems) {
                boolean hasConfirmedOut = order_InventoryLogRepositroy
                    .existsByOrderLineItem_OrderLineItemIdAndLogTypeAndLogStatus(
                        oli.getOrderLineItemId(),
                        LogType.OUT,
                        LogStatus.CONFIRMED
                    );
                log.debug("[findOrderSummaries] orderLineItemId={}, hasConfirmedOut={}",
                        oli.getOrderLineItemId(), hasConfirmedOut);
                if (!hasConfirmedOut) {
                    allShipped = false;
                    break;
                }
            }

            // 2-3) DTO에 allShipped 값 세팅
            dto.setAllShipped(allShipped);
            log.debug("[findOrderSummaries] Set allShipped={} for orderId={}", allShipped, orderId);
        });

        log.debug("[findOrderSummaries] Returning page of summaries");
        return page;
    }

    /**
     * 주문서(orderId)에 속한 모든 품목(item)을 조회하고,
     * 각 아이템별로 “재고가 있는(>0)” 창고 정보를 Flat List<ItemWarehouseResponse> 형태로 반환.
     */
    @Transactional(readOnly = true)
    public List<ItemWarehouseResponse> getShipmentInfoList(Long orderId) {
        log.debug("[getShipmentInfoList] Fetching shipment info for orderId={}", orderId);

        // 1) 주문서에 속한 모든 OrderLineItem 조회
        List<OrderLineItem> lineItems = orderLineItemRepository
            .findByCompanyOrder_OrderId(orderId);
        log.debug("[getShipmentInfoList] Found {} line items for orderId={}", lineItems.size(), orderId);

        // 2) 각 OrderLineItem → Stock 조회 → ItemWarehouseResponse로 Flat Map
        List<ItemWarehouseResponse> responses = lineItems.stream()
            .flatMap(li -> {
                Long itemId = li.getItem().getId();
                String itemCode = li.getItem().getCode();
                String itemName = li.getItem().getName();
                Integer orderQty = li.getUnitQty();

                log.debug("[getShipmentInfoList] Processing OrderLineItem: orderLineItemId={}, itemId={}, orderQty={}",
                        li.getOrderLineItemId(), itemId, orderQty);

                List<Stock> stocks = stockRepository
                    .findByItemIdAndQuantityGreaterThan(itemId, 0);
                log.debug("[getShipmentInfoList] Retrieved {} stock entries for itemId={}", stocks.size(), itemId);

                Stream<ItemWarehouseResponse> itemWhStream = stocks.stream()
                    .map(stock -> {
                        Warehouse wh = stock.getWarehouse();
                        ItemWarehouseResponse resp = new ItemWarehouseResponse(
                            li.getOrderLineItemId(),
                            itemId,
                            itemCode,
                            itemName,
                            orderQty,
                            wh.getId(),
                            wh.getWarehouseName(),
                            stock.getQuantity()
                        );
                        log.debug("[getShipmentInfoList] Created ItemWarehouseResponse: {}", resp);
                        return resp;
                    });

                return itemWhStream;
            })
            .collect(Collectors.toList());

        log.debug("[getShipmentInfoList] Returning {} ItemWarehouseResponse entries", responses.size());
        return responses;
    }
    
    
    @Transactional
    public void updateOrder(Long orderId, CreateOrderRequest request) {
        // 1. 기존 주문 조회
        CompanyOrder order = companyOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));

        // 2. 주문 헤더 수정
        Company company = companyRepository.findById(request.getCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("고객사가 존재하지 않습니다."));

        order.setCompany(company);
        order.setOrderDate(request.getOrderDate());
        order.setOrderType(CompanyOrder.OrderType.valueOf(request.getOrderType()));
        order.setNote(request.getRemark());

        // 3. 기존 lineItems에서 clear()로 모두 삭제 (JPA 고아제거와 충돌없이)
        List<OrderLineItem> lineItems = order.getLineItems();
        lineItems.clear();

        int totalQty = 0;
        long totalAmount = 0;
        for (OrderItemRequest itemReq : request.getItems()) {
            Item item = itemRepository.findById(itemReq.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품목입니다."));

            OrderLineItem lineItem = OrderLineItem.builder()
                .item(item)
                .unit(itemReq.getUnit())
                .unitPrice(itemReq.getUnitPrice())
                .unitQty(itemReq.getUnitQty())
                .unitPriceall(itemReq.getUnitQty() * itemReq.getUnitPrice())
                .warehouse(null)
                .build();
            lineItem.setCompanyOrder(order);

            totalQty += itemReq.getUnitQty();
            totalAmount += itemReq.getUnitQty() * itemReq.getUnitPrice();

            lineItems.add(lineItem);
        }
        order.setOrderQty(totalQty);
        order.setAmount(totalAmount);

        companyOrderRepository.save(order);

        // 필요시: mrpService.generateMrpForOrder(orderId);
    }

}
