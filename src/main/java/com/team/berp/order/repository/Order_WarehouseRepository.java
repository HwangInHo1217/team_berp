package com.team.berp.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Warehouse;

public interface Order_WarehouseRepository extends JpaRepository<Warehouse, Long> {
	Optional<Warehouse> findBywarehouseName(String name);
}
