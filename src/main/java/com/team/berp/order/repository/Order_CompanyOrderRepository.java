// src/main/java/com/team/berp/order/repository/Order_CompanyOrderRepository.java
package com.team.berp.order.repository;

import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.order.dto.OrderSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface Order_CompanyOrderRepository extends JpaRepository<CompanyOrder, Long> {
    // (기존 findByFilters는 그대로 두시면 됩니다)

	@Query(value = """
		    SELECT DISTINCT new com.team.berp.order.dto.OrderSummaryDto(
		        co.orderId,
		        co.orderNum,
		        co.orderDate,
		        c.companyName,
		        c.companyEmpName,
		        e.empName,
		        co.orderQty,
		        co.amount
		    )
		    FROM CompanyOrder co
		    JOIN co.company c
		    JOIN c.employee e
		    LEFT JOIN co.lineItems li
		    WHERE co.orderType = 'CUSTOMER'  
		      AND (:companyId IS NULL OR c.companyId = :companyId)
		      AND (:itemId    IS NULL OR li.item.id     = :itemId)
		      AND (:fromDate  IS NULL OR co.orderDate >= :fromDate)
		      AND (:toDate    IS NULL OR co.orderDate <= :toDate)
		    """,
		    countQuery = """
		    SELECT COUNT(DISTINCT co)
		    FROM CompanyOrder co
		    JOIN co.company c
		    LEFT JOIN co.lineItems li
		    WHERE co.orderType = 'CUSTOMER'  
		      AND (:companyId IS NULL OR c.companyId = :companyId)
		      AND (:itemId    IS NULL OR li.item.id     = :itemId)
		      AND (:fromDate  IS NULL OR co.orderDate >= :fromDate)
		      AND (:toDate    IS NULL OR co.orderDate <= :toDate)
		    """
		)
		Page<OrderSummaryDto> findSummariesByFilters(
		    @Param("companyId") Long companyId,
		    @Param("itemId")    Long itemId,
		    @Param("fromDate")  LocalDate fromDate,
		    @Param("toDate")    LocalDate toDate,
		    Pageable pageable
		);

    // ✅ 올바른 방식 (실제 PK 필드명: orderId)
    void deleteByOrderIdIn(List<Long> orderIds);
 // OrderLineItemRepository.java
    List<OrderLineItem> findByOrderIdIn(List<Long> orderIds);

}