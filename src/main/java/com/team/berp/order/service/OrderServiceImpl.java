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
    public OrderPageDto getOrders(String companyName, String itemName,
                                  LocalDate dateFrom, LocalDate dateTo,
                                  Pageable pageable) {
        var page = orderRepo.findByFilters(
            companyName, itemName, dateFrom, dateTo, pageable);
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
        var items = o.getLineItems().stream()
            .map(li -> new OrderLineItemDto(
                li.getOrderLineItemId(),
                li.getItem().getName(),
                li.getItem().getUnit(),
                li.getUnitQty(),
                li.getUnitPrice(),
                li.getUnitPriceall()))
            .collect(Collectors.toList());

        return new OrderDto(
            o.getOrderNum(),
            o.getCompany().getCompanyName(),
            o.getCompany().getEmployee().getEmpName(),
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
        long cnt = orderRepo.countByOrderType(CompanyOrder.OrderType.CUSTOMER);
        Long nextNum = cnt + 1L;
        Company comp = companyRepo.findById(form.getCustomerId())
                          .orElseThrow();
        CompanyOrder o = new CompanyOrder();
        o.setOrderType(CompanyOrder.OrderType.CUSTOMER);
        o.setOrderNum(nextNum);
        o.setCompany(comp);
        o.setOrderDate(form.getOrderDate());
        o.setNote(Optional.ofNullable(form.getNote()).orElse(""));
        o = orderRepo.save(o);

        long totalQty = 0, totalAmt = 0;
        for (OrderLineItemDto dto : form.getItems()) {
            Item it = itemRepo.findByName(dto.getItemName());
            var li = new OrderLineItem();
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
        return getOrder(o.getOrderNum());
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
            var li = new OrderLineItem();
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
            if (o != null) {
                orderRepo.delete(o);
            }
        }
    }


    @Override public List<Company> getAllCompanies() { return companyRepo.findAll(); }
    @Override public List<Item>    getAllItems()     { return itemRepo.findAll(); }

    @Override
    public CompanyContactDto getCompanyContactInfo(Long customerId) {
        Company c = companyRepo.findById(customerId).orElseThrow();
        return new CompanyContactDto(
            c.getEmployee().getEmpName(),
            c.getCompanyEmpName()
        );
    }
}
