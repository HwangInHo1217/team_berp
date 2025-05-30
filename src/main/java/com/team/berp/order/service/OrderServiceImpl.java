// OrderServiceImpl.java
package com.team.berp.order.service;

import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.dto.OrderDto.LineItem;
import com.team.berp.order.repository.CompanyOrderRepository;
import com.team.berp.order.repository.OrderLineItemRepository;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.domain.Item;
import com.team.berp.domain.Company;
import org.springframework.data.domain.PageRequest;
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
    private final CompanyOrderRepository orderRepo;
    private final OrderLineItemRepository lineRepo;

    public OrderServiceImpl(
            CompanyOrderRepository orderRepo,
            OrderLineItemRepository lineRepo
    ) {
        this.orderRepo = orderRepo;
        this.lineRepo = lineRepo;
    }

    @Override
    public List<OrderDto> findByFilters(Long companyId, Long itemId, LocalDate fromDate, LocalDate toDate) {
        return orderRepo.findByFilters(companyId, itemId, fromDate, toDate, PageRequest.of(0, 10))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createOrder(OrderDto dto) {
        // 주문 번호 생성
        String prefix = "cus-";
        Long nextId = orderRepo.count() + 1;
        dto.setOrderNum(prefix + String.format("%03d", nextId));

        CompanyOrder entity = new CompanyOrder();
        Company company = new Company(); company.setCompanyId(dto.getCompanyId());
        entity.setCompany(company);
        entity.setOrderDate(dto.getOrderDate());
        entity.setRemark(dto.getRemark());
        entity.setOrderType(dto.getOrderType());

        // 저장 전 합계/수량 계산
        long totalAmount = 0;
        int totalQty = 0;
        List<OrderLineItem> lines = dto.getItems().stream().map(itemDto -> {
            OrderLineItem oli = new OrderLineItem();
            oli.setCompanyOrder(entity);

            Item item = new Item(); item.setId(itemDto.getItemId());
            oli.setItem(item);
            oli.setUnit(itemDto.getUnit());
            oli.setUnitPrice(itemDto.getUnitPrice());
            oli.setUnitQty(itemDto.getUnitQty());
            long sum = itemDto.getUnitPrice() * itemDto.getUnitQty();
            oli.setUnitPriceAll(sum);

            totalAmount += sum;
            totalQty += itemDto.getUnitQty();
            return oli;
        }).collect(Collectors.toList());

        entity.setAmount(totalAmount);
        entity.setOrderQty(totalQty);
        entity.setOrderNum(dto.getOrderNum());

        // 저장
        CompanyOrder saved = orderRepo.save(entity);
        lines.forEach(lineRepo::save);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderDetail(Long orderId) {
        return orderRepo.findById(orderId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
    }

    @Override
    @Transactional
    public OrderDto updateOrder(OrderDto dto) {
        CompanyOrder entity = orderRepo.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        entity.setOrderDate(dto.getOrderDate());
        entity.setRemark(dto.getRemark());

        // 기존 품목 삭제 후 재저장
        entity.getOrderLineItems().forEach(lineRepo::delete);
        entity.getOrderLineItems().clear();
        createOrder(dto); // 재생성
        return dto;
    }

    @Override
    @Transactional
    public void deleteOrders(List<Long> orderIds) {
        orderIds.forEach(orderRepo::deleteById);
    }

    private OrderDto toDto(CompanyOrder co) {
        OrderDto dto = new OrderDto();
        dto.setOrderId(co.getOrderId());
        dto.setOrderNum(co.getOrderNum());
        dto.setOrderDate(co.getOrderDate());
        dto.setCompanyId(co.getCompany().getCompanyId());
        dto.setCompanyName(co.getCompany().getCompanyName());
        dto.setEmpName(co.getCompany().getEmployee().getEmpName());
        dto.setCompanyEmpName(co.getCompany().getEmployee().getCompanyEmpName());
        dto.setOrderType(co.getOrderType());
        dto.setItemType("product");
        dto.setAmount(co.getAmount());
        dto.setOrderQty(co.getOrderQty());
        dto.setRemark(co.getRemark());
        dto.setItems(
            co.getOrderLineItems().stream().map(oli -> {
                LineItem li = new LineItem();
                li.setOrderLineItemId(oli.getOrderLineItemId());
                li.setItemId(oli.getItem().getId());
                li.setItemName(oli.getItem().getName());
                li.setUnit(oli.getUnit());
                li.setUnitPrice(oli.getUnitPrice());
                li.setUnitQty(oli.getUnitQty());
                li.setUnitPriceAll(oli.getUnitPriceAll());
                return li;
            }).collect(Collectors.toList())
        );
        return dto;
    }
}