package com.team.berp.mrp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team.berp.domain.Mrp;

public interface EntityMrpRepository extends JpaRepository<Mrp, Integer> {
	
	@Query(value = """
	        SELECT unit_qty
	        FROM order_line_item oli
	        JOIN company_order co ON oli.order_id = co.order_id
	        WHERE oli.item_id = :itemId
	        ORDER BY co.order_date DESC
	        LIMIT 1
	    """, nativeQuery = true)
	    Integer findLatestOrderQtyByItemId(@Param("itemId") Long itemId);
	
	@Query(
			  value = "SELECT c.company_name " +
			          "FROM order_line_item oli " +
			          "JOIN company_order co ON oli.order_id = co.order_id " +
			          "JOIN company c ON co.company_id = c.company_id " +
			          "WHERE oli.item_id = :item_id " +
			          "ORDER BY co.order_date DESC LIMIT 1",
			  nativeQuery = true
			)
			String findLatestCompanyNameByItemId(@Param("item_id") Long item_id);

    // itemName 기준 정렬 (DESC/ASC 선택 가능, 기본 DESC)
    @Query(
        "SELECT m FROM Mrp m " +
        "JOIN m.item i " +
        "ORDER BY i.name DESC"
    )
    Page<Mrp> findAllOrderByItemNameDesc(Pageable pageable);

    // ASC 예시
    @Query(
        "SELECT m FROM Mrp m " +
        "JOIN m.item i " +
        "ORDER BY i.name ASC"
    )
    Page<Mrp> findAllOrderByItemNameAsc(Pageable pageable);
    
    Page<Mrp> findAll(Pageable pageable);
}

