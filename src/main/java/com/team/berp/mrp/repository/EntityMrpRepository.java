// File: src/main/java/com/team/berp/mrp/repository/EntityMrpRepository.java
package com.team.berp.mrp.repository;

import com.team.berp.domain.Mrp;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EntityMrpRepository extends JpaRepository<Mrp, Long>, JpaSpecificationExecutor<Mrp> {

    /**
     * 가장 최근 주문의 주문수량(unit_qty)을 조회합니다.
     */
    @Query(value = """
            SELECT oli.unit_qty
              FROM order_line_item oli
              JOIN company_order   co ON oli.order_id = co.order_id
             WHERE oli.item_id = :itemId
             ORDER BY co.order_date DESC
             LIMIT 1
            """, nativeQuery = true)
    Integer findLatestOrderQtyByItemId(@Param("itemId") Long itemId);

    /**
     * 가장 최근 주문의 거래처(company_name)를 조회합니다.
     */
    @Query(value = """
            SELECT c.company_name
              FROM order_line_item oli
              JOIN company_order   co ON oli.order_id = co.order_id
              JOIN company         c  ON co.company_id = c.company_id
             WHERE oli.item_id = :itemId
             ORDER BY co.order_date DESC
             LIMIT 1
            """, nativeQuery = true)
    String findLatestCompanyNameByItemId(@Param("itemId") Long itemId);

    /**
     * 품목명(item.name) 내림차순 정렬으로 페이징 조회
     */
    @Query("SELECT m FROM Mrp m JOIN m.item i ORDER BY i.name DESC")
    Page<Mrp> findAllOrderByItemNameDesc(Pageable pageable);

    /**
     * 품목명(item.name) 오름차순 정렬으로 페이징 조회
     */
    @Query("SELECT m FROM Mrp m JOIN m.item i ORDER BY i.name ASC")
    Page<Mrp> findAllOrderByItemNameAsc(Pageable pageable);
    
    Page<Mrp> findByBaseDateBetweenAndItem_CodeContainingIgnoreCaseOrBaseDateBetweenAndItem_NameContainingIgnoreCase(
            LocalDate startDate1, LocalDate endDate1, String itemCode, 
            LocalDate startDate2, LocalDate endDate2, String itemName, 
            Pageable pageable
    );

    // ← 아래 메서드를 리포지토리에 추가하세요!  
    List<Mrp> findByPlan_PlanId(Long planId);
}
