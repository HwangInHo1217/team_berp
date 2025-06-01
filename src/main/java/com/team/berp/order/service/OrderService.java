package com.team.berp.order.service;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.domain.Company;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.CompanyOrder.OrderType;
import com.team.berp.domain.Item;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.domain.Stock;
import com.team.berp.domain.Warehouse;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import com.team.berp.order.dto.CreateOrderRequest;
import com.team.berp.order.dto.OrderDetailResponse;
import com.team.berp.order.dto.OrderItemRequest;
import com.team.berp.order.dto.OrderSummaryDto;
import com.team.berp.order.dto.ItemWarehouseResponse;
import com.team.berp.order.repository.Order_CompanyOrderRepository;
import com.team.berp.order.repository.Order_CompanyRepository;
import com.team.berp.order.repository.Order_ItemRepository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import com.team.berp.order.repository.Order_StockRepository;
import com.team.berp.order.repository.Order_WarehouseRepository;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class OrderService {

    // 🔧 Repository 주입 (고객사, 주문, 품목, 창고, 주문 상세 등)
    private final Order_CompanyOrderRepository companyOrderRepository;
    private final Order_OrderLineItemRepository orderLineItemRepository;
    private final Order_CompanyRepository companyRepository;
    private final Order_ItemRepository itemRepository;
    private final Order_WarehouseRepository warehouseRepo;
    private final InventoryLogRepository inventoryLogRepository;
    private final Order_StockRepository stockRepository;
    // ✅ 주문번호 생성 로직: 날짜 + UUID 앞 4자리
    public String generateOrderNum() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4);
        return "ORD-" + datePart + "-" + randomPart;
    }

    // ✅ 주문 등록 처리 (주문 헤더 + 주문 상세)
    @Transactional
    public void createOrder(CreateOrderRequest request) {
        long totalQty = 0;
        long totalAmount = 0;

        // 🔹 전체 수량 및 총액 계산
        for (OrderItemRequest itemReq : request.getItems()) {
            totalQty += itemReq.getUnitQty();
            totalAmount += itemReq.getUnitQty() * itemReq.getUnitPrice();
        }

        // 🔹 고객사 정보 조회 및 예외 처리
        Company company = companyRepository.findById(request.getCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고객사입니다."));

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

        // 🔹 주문 상세 목록 구성
        List<OrderLineItem> lineItems = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            // 품목 ID 기준으로 품목 조회
            Item item = itemRepository.findById(itemReq.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품목입니다."));

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
            lineItem.setCompanyOrder(order);  // 🔥 주인 설정 (필수)

            lineItems.add(lineItem);
        }

        // 🔹 양방향 연관관계 설정 (order.setLineItems)
        order.setLineItems(lineItems);

        // 🔹 저장 (cascade 설정되어 있으면 lineItems도 함께 저장됨)
        companyOrderRepository.save(order);
    }

    // ✅ 단건 주문 조회 (상세보기용)
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId) {
        return companyOrderRepository.findById(orderId)
            .map(this::toDto)  // 조회 결과를 DTO로 변환
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
    }

    // ✅ 주문 도메인을 DTO로 변환 (헤더 + 상세 포함)
    private OrderDetailResponse toDto(CompanyOrder co) {
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
       // dto.setEmpName();
        dto.setCompanyEmpName(co.getCompany().getCompanyEmpName());

        // 🔹 주문 상세 DTO 리스트 세팅
        dto.setItems(co.getLineItems().stream()
            .map(this::toLineItemDto)
            .collect(Collectors.toList()));

        return dto;
    }

    // ✅ 주문 상세(OrderLineItem)를 DTO로 변환
    private OrderDetailResponse.LineItem toLineItemDto(OrderLineItem oli) {
        OrderDetailResponse.LineItem li = new OrderDetailResponse.LineItem();
        li.setOrderLineItemId(oli.getOrderLineItemId());
        li.setItemId(oli.getItem().getId());
        li.setItemName(oli.getItem().getName());   // ✅ 추가
        li.setUnit(oli.getItem().getUnit());       // ✅ 추가
        li.setUnitQty(oli.getUnitQty());
        li.setUnitPrice(oli.getUnitPrice());
        li.setUnitPriceAll(oli.getUnitPriceall());
        li.setWarehouseId(oli.getWarehouse() != null ? oli.getWarehouse().getId() : null);
        return li;
    }

    // ✅ 여러 주문 삭제
    @Transactional
    public void deleteOrders(List<Long> orderIds) {
        List<OrderLineItem> lineItems = orderLineItemRepository.findByCompanyOrderOrderIdIn(orderIds);

        List<OrderLineItem> deletableLineItems = new ArrayList<>();
        List<Long> blockedOrderIds = new ArrayList<>();

        for (OrderLineItem lineItem : lineItems) {
            boolean exists = inventoryLogRepository.existsByOrderLineItem_OrderLineItemId(lineItem.getOrderLineItemId());
            if (!exists) {
                deletableLineItems.add(lineItem);
            } else {
                blockedOrderIds.add(lineItem.getCompanyOrder().getOrderId());
            }
        }

        List<Long> deletableLineItemIds = deletableLineItems.stream()
                .map(OrderLineItem::getOrderLineItemId)
                .collect(Collectors.toList());

        if (!deletableLineItemIds.isEmpty()) {
            orderLineItemRepository.deleteAllByIdInBatch(deletableLineItemIds);
        }

        List<Long> deletableOrderIds = orderIds.stream()
                .filter(id -> !blockedOrderIds.contains(id))
                .collect(Collectors.toList());

        if (!deletableOrderIds.isEmpty()) {
            companyOrderRepository.deleteByOrderIdIn(deletableOrderIds);
        }

        // ❗차단된 주문 존재 시 에러 발생
        if (!blockedOrderIds.isEmpty()) {
            throw new RuntimeException("삭제 불가: 출고/입고 기록이 있는 주문이 포함되어 있습니다. (orderIds: " + blockedOrderIds + ")");
        }
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
        // 🔹 조건 기반 목록 조회 (요약 정보 DTO)
        return companyOrderRepository.findSummariesByFilters(companyId, itemId, fromDate, toDate, pageable);
    }
    
    /**
     * 주문서(orderId)에 속한 모든 품목(item)을 조회하고,
     * 각 아이템별로 “재고가 있는(>0)” 창고 정보를 Flat List<ItemWarehouseResponse> 형태로 반환.
     *
     * @param orderId 주문서 ID
     * @return List<ItemWarehouseResponse> (Flat List). 재고가 하나도 없는 경우 빈 List 반환.
     */
    @Transactional(readOnly = true)
    public List<ItemWarehouseResponse> getShipmentInfoList(Long orderId) {
        // 1) 주문서에 속한 모든 OrderLineItem 조회
        List<OrderLineItem> lineItems =
            orderLineItemRepository.findByCompanyOrder_OrderId(orderId);

        // 2) 각 OrderLineItem -> Stock 조회 -> ItemWarehouseResponse로 Flat Map
        return lineItems.stream()
            // 각 OrderLineItem(li)마다 “해당 품목(itemId)에 재고 > 0인 Stock 리스트”를 가져와서
            // 그 안에서 다시 Stream<ItemWarehouseResponse> 로 바꾼 뒤, 모두 이어 붙인다(flatMap).
            .flatMap(li -> {
                // 2-1) 품목 ID, 품목 이름 뽑기
                Long   itemId   = li.getItem().getId();
                String itemName = li.getItem().getName();

                // 2-2) itemId에 해당하는 Stock 중 quantity>0인 것만 가져온다
                List<Stock> stocks = stockRepository.findByItemIdAndQuantityGreaterThan(itemId, 0);

                // 2-3) Stock -> ItemWarehouseResponse 매핑 람다
                //      - stocks.stream() 타입은 Stream<Stock>
                //      - map(...) 을 통해 Stream<ItemWarehouseResponse>를 리턴해야 한다.
                Stream<ItemWarehouseResponse> itemWhStream = stocks.stream()
                    .map(stock -> {
                        Warehouse wh = stock.getWarehouse();
                        return new ItemWarehouseResponse(
                            itemId,
                            itemName,
                            wh.getId(),
                            wh.getWarehouseName(),
                            stock.getQuantity()
                        );
                    });

                // 반드시 Stream<ItemWarehouseResponse>를 리턴해야 한다!
                return itemWhStream;
            })
            // 3) flatMap으로 모은 Stream<ItemWarehouseResponse>를 List로 모은다
            .collect(Collectors.toList());
    }

}
