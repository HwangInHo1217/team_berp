// File: src/main/java/com/team/berp/order/repository/Order_CompanyOrderRepository.java
package com.team.berp.order.repository;

import com.team.berp.order.dto.OrderDto;
import com.team.berp.domain.CompanyOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * CompanyOrder 조회, 커스텀 JPQL
 */
public interface Order_CompanyOrderRepository extends JpaRepository<CompanyOrder, Long> {

    @Query(value =
        "SELECT new com.team.berp.order.dto.OrderDto(" +
        "  o.orderNum, co.companyName, emp.empName, co.companyEmpName, " +
        "  o.orderDate, COUNT(li), SUM(li.unitPriceall), o.note" +
        ") " +
        "FROM CompanyOrder o " +
        " JOIN o.company co " +
        " JOIN co.employee emp " +
        " JOIN o.lineItems li " +
        " JOIN li.item it " +
        "WHERE (:companyName IS NULL OR co.companyName = :companyName) " +
        "  AND (:itemName    IS NULL OR it.name        = :itemName) " +
        "  AND (:dateFrom    IS NULL OR o.orderDate   >= :dateFrom) " +
        "  AND (:dateTo      IS NULL OR o.orderDate   <= :dateTo) " +
        "GROUP BY o.orderNum, co.companyName, emp.empName, co.companyEmpName, o.orderDate, o.note",
      countQuery =
        "SELECT COUNT(DISTINCT o) FROM CompanyOrder o " +
        " JOIN o.company co JOIN o.lineItems li JOIN li.item it " +
        "WHERE (:companyName IS NULL OR co.companyName = :companyName) " +
        "  AND (:itemName    IS NULL OR it.name        = :itemName) " +
        "  AND (:dateFrom    IS NULL OR o.orderDate   >= :dateFrom) " +
        "  AND (:dateTo      IS NULL OR o.orderDate   <= :dateTo)"
    )
    Page<OrderDto> findByFilters(
        @Param("companyName") String companyName,
        @Param("itemName")    String itemName,
        @Param("dateFrom")    LocalDate dateFrom,
        @Param("dateTo")      LocalDate dateTo,
        Pageable pageable
    );

    @Query("SELECT o FROM CompanyOrder o " +
           "JOIN FETCH o.company co " +
           "JOIN FETCH co.employee emp " +
           "JOIN FETCH o.lineItems li " +
           "JOIN FETCH li.item it " +
           "WHERE o.orderNum = :orderNum")
    CompanyOrder findWithDetailsByOrderNum(@Param("orderNum") Long orderNum);

    CompanyOrder findByOrderNum(Long orderNum);

    long countByOrderType(CompanyOrder.OrderType orderType);
}