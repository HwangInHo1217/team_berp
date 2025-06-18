// File: src/main/java/com/team/berp/mrp/repository/EntityMrpRepository.java
package com.team.berp.mrp.repository;

import com.team.berp.domain.Mrp;
import com.team.berp.domain.MrpStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EntityMrpRepository extends JpaRepository<Mrp, Long>, JpaSpecificationExecutor<Mrp> {

    /**
     * (기존) 가장 최근 주문의 주문수량(unit_qty)을 조회합니다.
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
     * (기존) 가장 최근 주문의 거래처(company_name)를 조회합니다.
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
     * (기존) 품목명(item.name) 내림차순 정렬으로 페이징 조회
     */
    @Query("SELECT m FROM Mrp m JOIN m.plan p JOIN p.item i ORDER BY i.name DESC")
    Page<Mrp> findAllOrderByItemNameDesc(Pageable pageable);

    /**
     * (기존) 품목명(item.name) 오름차순 정렬으로 페이징 조회
     */
    @Query("SELECT m FROM Mrp m JOIN m.plan p JOIN p.item i ORDER BY i.name ASC")
    Page<Mrp> findAllOrderByItemNameAsc(Pageable pageable);

    /**
     * (수정) 기간 및 “완제품(item.code 또는 item.name)”으로 MRP 조회
     * └ 원래는 “m.item.code”였으나, 이를 “m.plan.item.code” (완제품) 으로 바꿔야 합니다.
     */
    @Query("""
            SELECT m
              FROM Mrp m
              JOIN m.plan p
              JOIN p.item i
             WHERE (m.baseDate BETWEEN :startDate1 AND :endDate1 AND i.code   LIKE %:itemSearch%) 
                OR (m.baseDate BETWEEN :startDate2 AND :endDate2 AND i.name   LIKE %:itemSearch%)
            """)
    Page<Mrp> findByBaseDateBetweenAndPlan_Item_CodeContainingIgnoreCaseOrBaseDateBetweenAndPlan_Item_NameContainingIgnoreCase(
            @Param("startDate1") LocalDate startDate1,
            @Param("endDate1")   LocalDate endDate1,
            @Param("itemSearch") String    itemSearch,
            @Param("startDate2") LocalDate startDate2,
            @Param("endDate2")   LocalDate endDate2,
            @Param("itemSearch") String    itemSearch2,
            Pageable pageable
    );

    /**
     * (기존) ProdPlan 기준으로 연결된 MRP 레코드를 조회할 때 쓰입니다.
     */
    List<Mrp> findByPlan_PlanId(Long planId);

    // ────────────────────────────────────────────────────────────────
    // 아래는 새로 추가된 메서드들 ↓
    // ────────────────────────────────────────────────────────────────

    /**
     * 1) 특정 itemId(완제품)에 대해, status가 PLANNED인 MRP 레코드들의 required_qty 합계를 구합니다.
     *    예: complete MRP 엔티티에서 받은 “필요수량”을 모두 더해서 한 번에 가져올 때 사용
     */
    @Query("""
            SELECT COALESCE(SUM(m.requiredQty), 0)
              FROM Mrp m
             WHERE m.plan.item.id = :itemId
               AND m.status = :status
            """)
    Integer sumRequiredQtyByItemIdAndStatus(
            @Param("itemId") Long itemId,
            @Param("status") MrpStatus status
    );

    /**
     * 2) 특정 itemId(완제품) + 기간(baseDate BETWEEN startDate AND endDate) 필터를 적용하여,
     *    status가 PLANNED인 MRP 레코드들의 required_qty 합계를 구합니다.
     *    예: 화면에서 “기간 입력 → 그 범위 내의 부족수량”을 보고 싶을 때 사용
     */
    @Query("""
            SELECT COALESCE(SUM(m.requiredQty), 0)
              FROM Mrp m
              JOIN m.plan p
              JOIN p.item i
             WHERE i.id = :itemId
               AND m.status = :status
               AND m.baseDate BETWEEN :startDate AND :endDate
            """)
    Integer sumRequiredQtyByItemIdAndStatusAndBaseDateBetween(
            @Param("itemId")    Long      itemId,
            @Param("status")    MrpStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );

    /**
     * 3) (추가 옵션) 특정 itemId(완제품)에 대해 모든 MRP 레코드를 합산 (상태나 기간 조건 없이 합계만 필요할 때 사용)
     */
    @Query("""
            SELECT COALESCE(SUM(m.requiredQty), 0)
              FROM Mrp m
              JOIN m.plan p
              JOIN p.item i
             WHERE i.id = :itemId
            """)
    Integer sumRequiredQtyByItemId(@Param("itemId") Long itemId);
    
    Page<Mrp> findByStatusAndBaseDateBetween(
            MrpStatus status,
            LocalDate start,
            LocalDate end,
            Pageable pageable
        );

	Page<Mrp> findByStatusAndBaseDateBetweenAndPlan_Item_CodeContainingIgnoreCaseOrBaseDateBetweenAndPlan_Item_NameContainingIgnoreCase(
	MrpStatus status,
	LocalDate from1, LocalDate to1, String code,
	LocalDate from2, LocalDate to2, String name,
	Pageable pageable
	);
    
	List<Mrp> findByPlan_Item_CodeAndStatus(String itemCode, MrpStatus status);
	
	Page<Mrp> findByStatusAndDueDateBetweenAndPlan_Item_CodeContainingIgnoreCaseOrStatusAndDueDateBetweenAndPlan_Item_NameContainingIgnoreCase(
		    MrpStatus status1, LocalDate startDate1, LocalDate endDate1, String itemCode,
		    MrpStatus status2, LocalDate startDate2, LocalDate endDate2, String itemName,
		    Pageable pageable
		);
	
	// [신규 메소드 추가] JOIN FETCH를 사용하여 연관된 엔티티를 한 번에 가져오도록 수정
    @Query("""
            SELECT m FROM Mrp m
            JOIN FETCH m.plan p
            JOIN FETCH p.item i
            WHERE m.status = :status
              AND m.dueDate BETWEEN :start AND :end
            ORDER BY m.mrpId DESC
            """)
    List<Mrp> findAllByStatusAndDueDateBetween(@Param("status") MrpStatus status, 
                                               @Param("start") LocalDate start, 
                                               @Param("end") LocalDate end);

    // 2. [신규 추가] 날짜와 키워드로 검색 시, Page가 아닌 List를 반환하는 메소드
    @Query("""
            SELECT m FROM Mrp m
            JOIN FETCH m.plan p
            JOIN FETCH p.item i
            WHERE m.status = :status
              AND m.dueDate BETWEEN :startDate AND :endDate
              AND (i.code LIKE %:keyword% OR i.name LIKE %:keyword%)
            ORDER BY m.mrpId DESC
            """)
    List<Mrp> findAllByStatusAndDueDateBetweenAndPlanItemNameOrCode(
        @Param("status") MrpStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("keyword") String keyword
    );
	
    @Query("""
            SELECT m FROM Mrp m
            JOIN FETCH m.plan p
            JOIN FETCH p.item i
            WHERE m.status = :status
            ORDER BY m.mrpId DESC
            """)
    List<Mrp> findAllByStatus(@Param("status") MrpStatus status);
    
}
