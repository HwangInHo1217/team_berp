package com.example.order.repository;

import com.example.order.dto.OrderDto;
import com.example.order.entity.CompanyOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<CompanyOrder, Long> {
    /**
     * 주문 목록 조회 (필터 적용, 페이징)
     */
    @Query(value = "SELECT new com.example.order.dto.OrderDto(" +
            " o.orderNum, co.companyName, e.empName, co.companyEmpName, o.orderDate, COUNT(li), SUM(li.unitPriceAll) ) " +
            "FROM CompanyOrder o " +
            "JOIN o.company co " +
            "JOIN o.employee e " +
            "JOIN o.orderLineItems li " +
            "JOIN li.item i " +
            "WHERE (:companyName IS NULL OR co.companyName = :companyName) " +
            "AND (:itemName IS NULL OR i.itemName = :itemName) " +
            "AND (:dateFrom IS NULL OR o.orderDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR o.orderDate <= :dateTo) " +
            "GROUP BY o.orderNum, co.companyName, e.empName, co.companyEmpName, o.orderDate",
        countQuery = "SELECT COUNT(DISTINCT o.orderNum) " +
            "FROM CompanyOrder o " +
            "JOIN o.company co " +
            "JOIN o.orderLineItems li " +
            "JOIN li.item i " +
            "WHERE (:companyName IS NULL OR co.companyName = :companyName) " +
            "AND (:itemName IS NULL OR i.itemName = :itemName) " +
            "AND (:dateFrom IS NULL OR o.orderDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR o.orderDate <= :dateTo)"
    )
    Page<OrderDto> findByFilters(
            @Param("companyName") String companyName,
            @Param("itemName") String itemName,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable);

    /**
     * 특정 orderNum에 해당하는 주문(엔티티 + 연관항목) 상세 조회
     */
    @Query("SELECT o FROM CompanyOrder o " +
           "JOIN FETCH o.company co " +
           "JOIN FETCH o.employee e " +
           "JOIN FETCH o.orderLineItems li " +
           "JOIN FETCH li.item i " +
           "WHERE o.orderNum = :orderNum")
    CompanyOrder findWithDetailsByOrderNum(@Param("orderNum") String orderNum);

    /**
     * 특정 주문유형의 주문 개수 (번호 생성용)
     */
    long countByOrderType(String orderType);

    /**
     * orderNum으로 엔티티 조회
     */
    CompanyOrder findByOrderNum(String orderNum);
}