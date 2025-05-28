package com.example.order.service;

import com.example.order.dto.OrderDto;
import com.example.order.dto.OrderLineItemDto;
import com.example.order.dto.OrderRegisterFormDto;
import com.example.order.entity.Company;
import com.example.order.entity.CompanyOrder;
import com.example.order.entity.OrderLineItem;
import com.example.order.entity.Item;
import com.example.order.repository.CompanyRepository;
import com.example.order.repository.ItemRepository;
import com.example.order.repository.OrderLineItemRepository;
import com.example.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepo;
    private final CompanyRepository companyRepo;
    private final ItemRepository itemRepo;
    private final OrderLineItemRepository oliRepo;

    public OrderServiceImpl(OrderRepository orderRepo,
                            CompanyRepository companyRepo,
                            ItemRepository itemRepo,
                            OrderLineItemRepository oliRepo) {
        this.orderRepo = orderRepo;
        this.companyRepo = companyRepo;
        this.itemRepo = itemRepo;
        this.oliRepo = oliRepo;
    }

    /**
     * 필터에 맞는 주문 목록 조회
     */
    @Override
    public List<OrderDto> getOrders(String companyName, String itemName,
                                    LocalDate dateFrom, LocalDate dateTo) {
        // 페이징 없이 전체 목록 반환 (필요 시 PageRequest.of 적용)
        Page<OrderDto> page = orderRepo.findByFilters(
                companyName, itemName, dateFrom, dateTo,
                PageRequest.of(0, 100));
        return page.getContent();
    }

    /**
     * 단일 주문 및 품목 상세 조회
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(String orderNum) {
        CompanyOrder order = orderRepo.findWithDetailsByOrderNum(orderNum);
        List<OrderLineItemDto> items = order.getOrderLineItems()
            .stream()
            .map(li -> new OrderLineItemDto(
                li.getOrderLineItemId(),
                li.getItem().getItemName(),
                li.getUnit(),
                li.getUnitPrice(),
                li.getUnitQty(),
                li.getUnitPriceAll()))
            .collect(Collectors.toList());
        return new OrderDto(
                order.getOrderNum(),
                order.getCompany().getCompanyName(),
                order.getEmployee().getEmpName(),
                order.getCompany().getCompanyEmpName(),
                order.getOrderDate(),
                order.getRemark(),
                items);
    }

    /**
     * 주문 등록
     */
    @Override
    @Transactional
    public OrderDto registerOrder(OrderRegisterFormDto form) {
        // 주문번호 생성 (cus-XXX)
        long count = orderRepo.countByOrderType(form.getOrderType());
        String orderNum = String.format("cus-%03d", count + 1);

        Company company = companyRepo.findByCompanyName(form.getCompanyName());
        // emp 및 companyEmp는 fetch join 로 상세조회 시 사용

        CompanyOrder order = new CompanyOrder();
        order.setOrderType(form.getOrderType());
        order.setOrderNum(orderNum);
        order.setCompany(company);
        // 임시: 대표 사원 지정 (추후 양방향 매핑 등 설정 필요)
        order.setEmployee(company.getEmployee());
        order.setOrderDate(form.getOrderDate());
        order.setRemark(form.getRemark());
        order = orderRepo.save(order);

        // 품목 등록
        form.getItems().forEach(dto -> {
            Item item = itemRepo.findByItemName(dto.getItemName());
            OrderLineItem oli = new OrderLineItem();
            oli.setOrder(order);
            oli.setItem(item);
            oli.setUnit(item.getUnit());
            oli.setUnitPrice(dto.getUnitPrice());
            oli.setUnitQty(dto.getUnitQty());
            oli.setUnitPriceAll(dto.getUnitPrice() * dto.getUnitQty());
            oliRepo.save(oli);
        });

        return getOrder(orderNum);
    }

    /**
     * 주문 수정
     */
    @Override
    @Transactional
    public OrderDto updateOrder(String orderNum, OrderRegisterFormDto form) {
        CompanyOrder order = orderRepo.findWithDetailsByOrderNum(orderNum);
        order.setOrderDate(form.getOrderDate());
        order.setRemark(form.getRemark());

        // 기존 품목 삭제 후 재등록
        oliRepo.deleteAll(order.getOrderLineItems());
        order.getOrderLineItems().clear();
        form.getItems().forEach(dto -> {
            Item item = itemRepo.findByItemName(dto.getItemName());
            OrderLineItem oli = new OrderLineItem();
            oli.setOrder(order);
            oli.setItem(item);
            oli.setUnit(item.getUnit());
            oli.setUnitPrice(dto.getUnitPrice());
            oli.setUnitQty(dto.getUnitQty());
            oli.setUnitPriceAll(dto.getUnitPrice() * dto.getUnitQty());
            oliRepo.save(oli);
        });

        return getOrder(orderNum);
    }

    /**
     * 주문 삭제
     */
    @Override
    @Transactional
    public void deleteOrders(List<String> orderNums) {
        orderNums.forEach(num -> {
            CompanyOrder order = orderRepo.findByOrderNum(num);
            orderRepo.delete(order);
        });
    }

    /**
     * 고객사 목록 조회
     */
    @Override
    public List<Company> getAllCompanies() {
        return companyRepo.findAll();
    }

    /**
     * 품목 목록 조회
     */
    @Override
    public List<Item> getAllItems() {
        return itemRepo.findAll();
    }

    /**
     * 고객사 기반 담당자 정보 제공
     */
    @Override
    public Map<String, String> getCompanyContactInfo(String companyName) {
        Company company = companyRepo.findByCompanyName(companyName);
        Map<String, String> info = new HashMap<>();
        info.put("empName", company.getEmployee().getEmpName());
        info.put("companyEmpName", company.getCompanyEmpName());
        return info;
    }
}