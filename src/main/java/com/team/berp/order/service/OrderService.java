package com.example.order.service;

import com.example.order.dto.OrderDto;
import com.example.order.dto.OrderRegisterFormDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderService {
    List<OrderDto> getOrders(String companyName,
                              String itemName,
                              LocalDate dateFrom,
                              LocalDate dateTo);

    OrderDto getOrder(String orderNum);

    OrderDto registerOrder(OrderRegisterFormDto form);

    OrderDto updateOrder(String orderNum, OrderRegisterFormDto form);

    void deleteOrders(List<String> orderNums);

    List<com.example.order.entity.Company> getAllCompanies();
    List<com.example.order.entity.Item> getAllItems();

    /**
     * 고객사 선택 시 empName, companyEmpName 정보 반환
     */
    Map<String, String> getCompanyContactInfo(String companyName);
}