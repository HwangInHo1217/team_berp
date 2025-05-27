package com.team.berp.order.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.team.berp.domain.Company;
import com.team.berp.domain.Item;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.order.dto.OrderRegisterFormDto;
import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.dto.OrderPageDto;
import com.team.berp.order.repository.Order_CompanyOrderRepository;
import com.team.berp.order.repository.Order_CompanyRepository;
import com.team.berp.order.repository.Order_EmployeeRepository;
import com.team.berp.order.repository.Order_ItemRepository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;

/**
 * 주문 서비스: 주문 헤더 및 상세 라인 저장과 조회 로직 구현
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
    private final Order_CompanyRepository      companyRepo;
    private final Order_EmployeeRepository     employeeRepo;
    private final Order_ItemRepository         itemRepo;
    private final Order_CompanyOrderRepository orderRepo;
    private final Order_OrderLineItemRepository oliRepo;

    public List<Company> findAllCompanies() {
        return companyRepo.findAll();
    }

    public List<Item> findAllItems() {
        return itemRepo.findAll();
    }
    

    /**
     * 주문 헤더 및 상세 라인 저장
     */
    public void registerOrder(OrderRegisterFormDto form) {
        // 1) 주문 헤더 생성 및 저장
        CompanyOrder order = new CompanyOrder();
        Company company = companyRepo.findById(form.getCompanyId())
            .orElseThrow(() -> new NoSuchElementException("Company not found: " + form.getCompanyId()));
        order.setCompany(company);
        order.setOrderDate(form.getOrderDate());
        order.setDueDate(form.getDueDate());
        order.setNote(form.getNote());
        order.setOrderType(form.getOrderType());

        // 테이블에 렌더링된 행 개수
        order.setUnitQty(form.getUnitQty().longValue());
        // 발주 수량은 각 행의 수량 합
        long totalQty = form.getOrderQty().stream()
            .mapToLong(Integer::longValue)
            .sum();
        order.setOrderQty(totalQty);

        // 금액은 수량 * 단가 합산
        long totalAmount = 0;
        for (int i = 0; i < form.getItemId().size(); i++) {
            long qty = form.getOrderQty().get(i);
            long price = form.getUnitPrice().get(i);
            totalAmount += qty * price;
        }
        order.setAmount(totalAmount);

        orderRepo.save(order);
        
        // 2) 상세 라인 저장
        for (int i = 0; i < form.getItemId().size(); i++) {
            OrderLineItem li = new OrderLineItem();
            li.setOrder(order);
            Item item = itemRepo.findById(form.getItemId().get(i)).orElseThrow();
            li.setItem(item);
            oliRepo.save(li);
        }
        
    }

    /**
     * 모든 주문 목록을 DTO로 반환
     */
    public List<OrderDto> findAllOrders() {
        return oliRepo.findAllWithAllRelations().stream()
            .map(allOrderList -> {
            	OrderDto dto = new OrderDto();
            	dto.setOrderLineItemId(allOrderList.getOrderLineItemId());
            	dto.setOrderId(allOrderList.getOrder().getOrderId());
            	dto.setOrderType(allOrderList.getOrder().getOrderType());
            	dto.setOrderQty(allOrderList.getOrder().getOrderQty());
            	dto.setOrderDate(allOrderList.getOrder().getOrderDate());
            	dto.setUnitQty(allOrderList.getOrder().getUnitQty());
            	dto.setAmount(allOrderList.getOrder().getAmount());
            	dto.setDueDate(allOrderList.getOrder().getDueDate());
            	dto.setNote(allOrderList.getOrder().getNote());
            	dto.setCompanyId(allOrderList.getOrder().getCompany().getCompanyId());
            	dto.setCompanyName(allOrderList.getOrder().getCompany().getCompanyName());
            	dto.setCompanyEmpName(allOrderList.getOrder().getCompany().getCompanyEmpName());
            	dto.setEmployeeId(allOrderList.getOrder().getCompany().getEmployee().getEmployeeId());
            	dto.setEmpName(allOrderList.getOrder().getCompany().getEmployee().getEmpName());
            	dto.setItemId(allOrderList.getItem().getId());
            	dto.setItemName(allOrderList.getItem().getName());
            	dto.setItemCode(allOrderList.getItem().getCode());
            	dto.setUnitPrice(allOrderList.getItem().getUnitPrice());
            	dto.setUnit(allOrderList.getItem().getUnit());
            	
            	return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * 단일 주문 상세 조회
     */
    public List<OrderDto> getOrderDetail(Long lineItemId) {
    	List<OrderLineItem> order_line_item_id_search = oliRepo.findByIdWithAllRelations(lineItemId);
        return order_line_item_id_search.stream().map(orderOne -> {
        	OrderDto dto = new OrderDto();
        	dto.setOrderLineItemId(orderOne.getOrderLineItemId());
        	dto.setOrderId(orderOne.getOrder().getOrderId());
        	dto.setOrderType(orderOne.getOrder().getOrderType());
        	dto.setOrderQty(orderOne.getOrder().getOrderQty());
        	dto.setOrderDate(orderOne.getOrder().getOrderDate());
        	dto.setUnitQty(orderOne.getOrder().getUnitQty());
        	dto.setAmount(orderOne.getOrder().getAmount());
        	dto.setDueDate(orderOne.getOrder().getDueDate());
        	dto.setNote(orderOne.getOrder().getNote());
        	dto.setCompanyId(orderOne.getOrder().getCompany().getCompanyId());
        	dto.setCompanyName(orderOne.getOrder().getCompany().getCompanyName());
        	dto.setCompanyEmpName(orderOne.getOrder().getCompany().getCompanyEmpName());
        	dto.setEmployeeId(orderOne.getOrder().getCompany().getEmployee().getEmployeeId());
        	dto.setEmpName(orderOne.getOrder().getCompany().getEmployee().getEmpName());
        	dto.setItemId(orderOne.getItem().getId());
        	dto.setItemName(orderOne.getItem().getName());
        	dto.setItemCode(orderOne.getItem().getCode());
        	dto.setUnitPrice(orderOne.getItem().getUnitPrice());
        	dto.setUnit(orderOne.getItem().getUnit());
        	
        	return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 단위 및 단가 정보 조회
     */
    public Map<String, Object> getUnitInfo(Long itemId) {
        Item item = itemRepo.findById(itemId)
            .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));
        Map<String, Object> map = new HashMap<>();
        map.put("unit", item.getUnit());
        map.put("unitPrice", item.getUnitPrice());
        return map;
    }
    
    
    
    public Page<OrderPageDto> getOrderPage(int page, int size) {
        Pageable pg = PageRequest.of(page, size);
        return oliRepo.findAllDtos(pg);
    }
}