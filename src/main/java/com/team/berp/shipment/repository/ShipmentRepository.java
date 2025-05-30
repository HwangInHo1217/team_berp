/*
 * Repository: ShipmentRepository.java
 * Purpose: JPA repository for InventoryLog entity, customized for shipment operations.
 */
package com.team.berp.shipment.repository;

import com.team.berp.domain.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for InventoryLog (shipment records).
 */
@Repository
public interface ShipmentRepository extends JpaRepository<InventoryLog, Long> {
    // We use standard findAll and findById;
    // custom filtering is handled in service layer.
}