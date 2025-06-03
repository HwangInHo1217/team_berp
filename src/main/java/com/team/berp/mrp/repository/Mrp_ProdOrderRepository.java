// File: /Team_BERP/src/main/java/com/team/berp/mrp/repository/ProdOrderRepository.java
package com.team.berp.mrp.repository;

import com.team.berp.domain.ProdOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Mrp_ProdOrderRepository extends JpaRepository<ProdOrder, Long> {
    // 특정 생산계획(plan_id)에 속한 모든 ProdOrder 조회
    List<ProdOrder> findByPlan_PlanId(Long planId);
}
