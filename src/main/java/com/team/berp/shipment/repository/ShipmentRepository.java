package com.team.berp.shipment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.InventoryLog;
import com.team.berp.domain.LogType;

public interface ShipmentRepository extends JpaRepository<InventoryLog, Long> {
	  // OUT(출고) 타입만 조회할 수 있도록 메서드 추가
    List<InventoryLog> findByLogTypeOrderByLogDatetimeDesc(LogType logType);
}
