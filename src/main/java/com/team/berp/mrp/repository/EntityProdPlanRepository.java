package com.team.berp.mrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team.berp.domain.ProdPlan;

public interface EntityProdPlanRepository extends JpaRepository<ProdPlan, Integer> {
	
	// 해당 품목의 최신 생산계획 수량 (가장 최근 plan_id 기준)
    @Query(value = """
        SELECT plan_qty
        FROM prod_plan
        WHERE item_id = :itemId
        ORDER BY plan_id DESC
        LIMIT 1
    """, nativeQuery = true)
    Integer findLatestProdQtyByItemId(@Param("itemId") Long itemId);
	
}
