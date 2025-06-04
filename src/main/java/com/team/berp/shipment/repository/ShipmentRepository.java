package com.team.berp.shipment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.InventoryLog;
import com.team.berp.domain.LogType;

public interface ShipmentRepository extends JpaRepository<InventoryLog, Long> {
	  // OUT(출고) 타입만 조회할 수 있도록 메서드 추가
	// InventoryLogRepository 인터페이스 안에 다음 메서드를 추가합니다.
	List<InventoryLog> findByLogTypeInOrderByLogDatetimeDesc(List<LogType> types);
}
