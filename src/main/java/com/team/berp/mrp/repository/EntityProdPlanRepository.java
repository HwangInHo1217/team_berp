package com.team.berp.mrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.mrp.domain.DomainProdPlan;

public interface EntityProdPlanRepository extends JpaRepository<DomainProdPlan, Integer> {
	//
}
