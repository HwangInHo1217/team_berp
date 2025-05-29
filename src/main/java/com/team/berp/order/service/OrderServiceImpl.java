// File: src/main/java/com/team/berp/order/service/OrderServiceImpl.java
package com.team.berp.order.service;

import com.team.berp.domain.*;
import com.team.berp.order.dto.*;
import com.team.berp.order.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final Order_CompanyOrderRepository orderRepo;
    private final Order_CompanyRepository      companyRepo;
    private final Order_EmployeeRepository     employeeRepo;
    private final Order_ItemRepository         itemRepo;
    private final Order_OrderLineItemRepository oliRepo;

    @Override
    public OrderPageDto getOrders(String companyName,
                                  String itemName,
                                  LocalDate dateFrom,
                                  LocalDate dateTo,
                                  Pageable pageable) {

        var page = orderRepo.findByFilters(companyName, itemName, dateFrom, dateTo, pageable);
        return new OrderPageDto(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderNum) {
        CompanyOrder o = orderRepo.findWithDetailsByOrderNum(orderNum);

        // 1) 라인 아이템 DTO 변환
        List<OrderLineItemDto> items = o.getLineItems().stream()
            .map(li -> new OrderLineItemDto(
                li.getOrderLineItemId(),
                li.getItem().getName(),
                li.getItem().getUnit(),
                li.getUnitPrice(),
                li.getUnitQty(),
                li.getUnitPriceall()
            ))
            .collect(Collectors.toList());

        // 2) OrderDto에 담아 리턴
        return new OrderDto(
            o.getOrderNum(),
            o.getCompany().getCompanyName(),
            employeeRepo.findById(o.getCompany().getEmployee().getEmployeeId())
                        .map(Employee::getEmpName).orElse(""),
            o.getCompany().getCompanyEmpName(),
            o.getOrderDate(),
            o.getOrderQty(),
            o.getAmount(),
            o.getNote(),
            items
        );
    }

    @Override
    @Transactional
    public OrderDto registerOrder(OrderRegisterFormDto form) {
        // 1) 다음 번호 계산 (cus-001 → 1 기반)
        long cnt = orderRepo.countByOrderType(CompanyOrder.OrderType.CUSTOMER);
        Long orderNum = cnt + 1;

        // 2) Company, Employee 세팅
        Company comp = companyRepo.findByCompanyName(form.getCompanyName());

        // 3) CompanyOrder 엔티티 생성
        CompanyOrder o = new CompanyOrder();
        o.setOrderType(CompanyOrder.OrderType.CUSTOMER);
        o.setOrderNum(orderNum);
        o.setCompany(comp);
        o.setOrderDate(form.getOrderDate());
        o.setNote(Optional.ofNullable(form.getNote()).orElse(""));
        o = orderRepo.save(o);

        // 4) 라인 아이템 저장 및 합산
        long totalQty = 0, totalAmt = 0;
        for (OrderLineItemDto dto : form.getItems()) {
            Item it = itemRepo.findByName(dto.getItemName());
            OrderLineItem li = new OrderLineItem();
            li.setCompanyOrder(o);
            li.setItem(it);
            li.setUnitQty(dto.getUnitQty());
            li.setUnitPrice(dto.getUnitPrice());
            li.setUnitPriceall(dto.getUnitQty() * dto.getUnitPrice());
            oliRepo.save(li);

            totalQty += dto.getUnitQty();
            totalAmt += dto.getUnitQty() * dto.getUnitPrice();
        }

        // 5) 합계, 수량 업데이트
        o.setOrderQty(totalQty);
        o.setAmount(totalAmt);
        orderRepo.save(o);

        return getOrder(orderNum);
    }

    @Override
    @Transactional
    public OrderDto updateOrder(Long orderNum, OrderRegisterFormDto form) {
        CompanyOrder o = orderRepo.findWithDetailsByOrderNum(orderNum);
        o.setOrderDate(form.getOrderDate());
        o.setNote(Optional.ofNullable(form.getNote()).orElse(""));
        oliRepo.deleteAll(o.getLineItems());

        long totalQty = 0, totalAmt = 0;
        for (OrderLineItemDto dto : form.getItems()) {
            Item it = itemRepo.findByName(dto.getItemName());
            OrderLineItem li = new OrderLineItem();
            li.setCompanyOrder(o);
            li.setItem(it);
            li.setUnitQty(dto.getUnitQty());
            li.setUnitPrice(dto.getUnitPrice());
            li.setUnitPriceall(dto.getUnitQty() * dto.getUnitPrice());
            oliRepo.save(li);
            totalQty += dto.getUnitQty();
            totalAmt += dto.getUnitQty() * dto.getUnitPrice();
        }

        o.setOrderQty(totalQty);
        o.setAmount(totalAmt);
        orderRepo.save(o);

        return getOrder(orderNum);
    }

    @Override
    @Transactional
    public void deleteOrders(List<Long> orderNums) {
        for (Long num : orderNums) {
            CompanyOrder o = orderRepo.findByOrderNum(num);
            orderRepo.delete(o);
        }
    }

    @Override public List<Company> getAllCompanies() { return companyRepo.findAll(); }
    @Override public List<Item>    getAllItems()     { return itemRepo.findAll(); }

    @Override
    public CompanyContactDto getCompanyContactInfo(String companyName) {
        Company c = companyRepo.findByCompanyName(companyName);
        return new CompanyContactDto(
            c.getEmployee().getEmpName(),
            c.getCompanyEmpName()
        );
    }
}