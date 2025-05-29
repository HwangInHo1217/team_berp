// File: src/main/java/com/team/berp/order/service/OrderService.java
package com.team.berp.order.service;

import com.team.berp.domain.Company;
import com.team.berp.domain.Item;
import com.team.berp.order.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 주문 관련 비즈니스 로직 */
public interface OrderService {
    OrderPageDto getOrders(String companyName,
                           String itemName,
                           LocalDate dateFrom,
                           LocalDate dateTo,
                           Pageable pageable);

    OrderDto getOrder(Long orderNum);

    OrderDto registerOrder(OrderRegisterFormDto form);

    OrderDto updateOrder(Long orderNum, OrderRegisterFormDto form);

    void deleteOrders(List<Long> orderNums);

    List<Company> getAllCompanies();
    List<Item>    getAllItems();

    /** 고객사 선택 시 our-emp, comp-emp 자동 채움용 */
    CompanyContactDto getCompanyContactInfo(String companyName);
}