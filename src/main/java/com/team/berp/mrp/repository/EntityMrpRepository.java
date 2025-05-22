package com.team.berp.mrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.mrp.domain.DomainMrp;

public interface EntityMrpRepository extends JpaRepository<DomainMrp, Integer> {
	//
}
