package com.team.berp.order.service;

import com.team.berp.domain.Company;
import com.team.berp.domain.Item;
import com.team.berp.order.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 관련 비즈니스 로직 인터페이스
 */
public interface OrderService {

    /** 1) 주문 목록 조회 (필터 + 페이징) */
    OrderPageDto getOrders(String companyName,
                           String itemName,
                           LocalDate dateFrom,
                           LocalDate dateTo,
                           Pageable pageable);

    /** 2) 주문 단일 건 상세 조회 */
    OrderDto getOrder(Long orderNum);

    /** 3) 주문 등록 */
    OrderDto registerOrder(OrderRegisterFormDto form);

    /** 4) 주문 수정 */
    OrderDto updateOrder(Long orderNum, OrderRegisterFormDto form);

    /** 5) 주문 삭제 (여러 건) */
    void deleteOrders(List<Long> orderNums);

    /** 6) 조회용: 모든 고객사 리스트 */
    List<Company> getAllCompanies();

    /** 7) 조회용: 모든 품목 리스트 */
    List<Item>    getAllItems();

    /** 8) 고객사 선택 시 내부/고객사 담당자 정보 채움용 */
    CompanyContactDto getCompanyContactInfo(Long customerId);
}
