// 5. com/team/berp/order/repository/OrderOrderLineItemRepository.java
package com.team.berp.order.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.dto.OrderPageDto;

/**
 * Repository for OrderLineItem entities (주문 상세 라인 관리).
 */
@Repository
public interface Order_OrderLineItemRepository extends JpaRepository<OrderLineItem, Long> {

    /**
     * 모든 주문 상세 라인과 관련된 헤더, 고객사, 담당자, 품목 정보를 한 번에 패치 조인으로 조회합니다.
     */
    @Query("SELECT oli FROM OrderLineItem oli " +
           "JOIN FETCH oli.order o " +
           "JOIN FETCH o.company c " +
           "JOIN FETCH c.employee e " +
           "JOIN FETCH oli.item i")
    List<OrderLineItem> findAllWithAllRelations();

    /**
     * 특정 주문 상세 라인 ID에 대한 모든 연관 정보를 조회합니다.
     * @param id 주문 상세 라인 PK
     */
    @Query("SELECT oli FROM OrderLineItem oli " +
           "JOIN FETCH oli.order o " +
           "JOIN FETCH o.company c " +
           "JOIN FETCH c.employee e " +
           "JOIN FETCH oli.item i " +
           "WHERE oli.orderLineItemId = :id")
    List<OrderLineItem> findByIdWithAllRelations(@Param("id") Long id);

    /**
     * 특정 주문 번호에 속한 상세 라인들을 조회합니다.
     * @param orderId 주문 헤더 PK
     */
    @Query("SELECT oli FROM OrderLineItem oli " +
           "WHERE oli.order.orderId = :orderId")
    List<OrderLineItem> findByOrderId(@Param("orderId") Long orderId);
    
    
    @Query("""
            SELECT new com.team.berp.order.dto.OrderPageDto(
               oli.orderLineItemId,
               co.orderId,
               c.companyName,
               e.empName,
               i.code,
               i.name,
               co.orderQty,
               i.unit,
               i.price,
               co.orderDate,
               co.dueDate,
               co.unitQty,
               co.amount
            )
            FROM OrderLineItem oli
            JOIN oli.order co
            JOIN co.company c
            JOIN c.employee e
            JOIN oli.item i
            """)
        Page<OrderPageDto> findAllDtos(Pageable pageable);
    
    
    /**
     * 특정 품목코드에 대한 단가를 조회합니다.
     * @param itemCode 품목코드
     * @return 단가
     */
    @Query("SELECT i.price FROM OrderLineItem oli WHERE oli.orderLineItemId = :id")
    Long findPriceByOrderLineItemId(@Param("id") Long id);
}
