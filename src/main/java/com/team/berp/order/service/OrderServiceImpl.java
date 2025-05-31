// OrderServiceImpl.java
package com.team.berp.order.service;

import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.dto.OrderSummaryDto;
import com.team.berp.order.repository.Order_CompanyOrderRepository;
import com.team.berp.order.repository.Order_CompanyRepository;
import com.team.berp.order.repository.Order_ItemRepository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import com.team.berp.order.repository.Order_WarehouseRepository;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.OrderLineItem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 비즈니스 로직 구현체
 */
@Service
public class OrderServiceImpl implements OrderService {
    private final Order_CompanyOrderRepository orderRepo;
    private final Order_OrderLineItemRepository lineRepo;
    private final Order_CompanyRepository         companyRepo;
    private final Order_ItemRepository            itemRepo;
    private final Order_WarehouseRepository       warehouseRepo;

    public OrderServiceImpl(
        Order_CompanyOrderRepository orderRepo,
        Order_OrderLineItemRepository lineRepo,
        Order_CompanyRepository companyRepo,
        Order_ItemRepository itemRepo,
        Order_WarehouseRepository warehouseRepo
    ) {
        this.orderRepo    = orderRepo;
        this.lineRepo     = lineRepo;
        this.companyRepo  = companyRepo;
        this.itemRepo     = itemRepo;
        this.warehouseRepo= warehouseRepo;
    }

    @Override
    @Transactional
    public void createOrder(OrderDto dto) {
        CompanyOrder co = new CompanyOrder();
        co.setCompany(companyRepo.findById(dto.getCompanyId()).orElseThrow());
        co.setOrderDate(dto.getOrderDate());
        co.setNote(dto.getRemark());
        co.setOrderType(CompanyOrder.OrderType.valueOf(dto.getOrderType()));

        List<OrderLineItem> lines = dto.getItems().stream()
            .map(liDto -> {
                OrderLineItem oli = new OrderLineItem();
                oli.setCompanyOrder(co);
                oli.setItem(itemRepo.findById(liDto.getItemId()).orElseThrow());
                oli.setWarehouse(
                        warehouseRepo.findById(liDto.getWarehouseId()).orElseThrow()
                    );
                oli.setUnitQty(liDto.getUnitQty().longValue());
                oli.setUnitPrice(liDto.getUnitPrice());
                oli.setUnitPriceall(liDto.getUnitPriceAll());
                return oli;
            })
            .collect(Collectors.toList());

        co.setLineItems(lines);
        co.setAmount(lines.stream().mapToLong(OrderLineItem::getUnitPriceall).sum());
        co.setOrderQty(lines.stream().mapToLong(oli -> oli.getUnitQty()).sum());
        orderRepo.save(co);
        lines.forEach(lineRepo::save);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderDetail(Long orderId) {
        return orderRepo.findById(orderId)
            .map(this::toDto)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
    }

    private OrderDto toDto(CompanyOrder co) {
        OrderDto dto = new OrderDto();
        dto.setOrderId(co.getOrderId());
        dto.setOrderNum(co.getOrderNum());
        dto.setOrderType(co.getOrderType().name());
        dto.setOrderDate(co.getOrderDate());
        dto.setOrderQty(co.getOrderQty());           // Long
        dto.setRemark(co.getNote());
        dto.setAmount(co.getAmount());
        dto.setCompanyId(co.getCompany().getCompanyId());

        dto.setItems(co.getLineItems().stream()
            .map(this::toLineItemDto)
            .collect(Collectors.toList()));
        return dto;
    }

    private OrderDto.LineItem toLineItemDto(OrderLineItem oli) {
        OrderDto.LineItem li = new OrderDto.LineItem();
        li.setItemId(oli.getItem().getId());
        li.setWarehouseId(oli.getWarehouse().getId());
        li.setUnitQty(oli.getUnitQty());
        li.setUnitPrice(oli.getUnitPrice());
        li.setUnitPriceAll(oli.getUnitPriceall());
        return li;
    }
    
    
    @Override
    @Transactional
    public OrderDto updateOrder(OrderDto dto) {
        CompanyOrder co = orderRepo.findById(dto.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        co.setOrderDate(dto.getOrderDate());
        co.setNote(dto.getRemark());
        co.getLineItems().forEach(lineRepo::delete);
        co.getLineItems().clear();
        createOrder(dto);
        return dto;
    }
    
    
    @Override
    @Transactional
    public void deleteOrders(List<Long> orderIds) {
        orderIds.forEach(orderRepo::deleteById);
    }
    
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> findOrderSummaries(
            Long companyId,
            Long itemId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        return orderRepo.findSummariesByFilters(companyId, itemId, fromDate, toDate, pageable);
    }

	@Override
	public List<OrderDto> findByFilters(Long companyId, Long itemId, LocalDate fromDate, LocalDate toDate) {
		// TODO Auto-generated method stub
		return null;
	}

}