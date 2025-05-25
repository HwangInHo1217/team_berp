package com.team.berp.mrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Stock;

public interface EntityStockRepository extends JpaRepository<Stock, Integer> {
	//
}
