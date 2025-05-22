package com.team.berp.mrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.mrp.domain.DomainStock;

public interface EntityStockRepository extends JpaRepository<DomainStock, Integer> {
	//
}
