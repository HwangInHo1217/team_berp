package com.team.berp.index;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.InventoryLog;
import com.team.berp.domain.LogType;

public interface Index_InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
	
	List<InventoryLog> findTop5ByLogTypeOrderByLogDatetimeDesc(LogType logType);
	
	List<InventoryLog> findTop5ByLogTypeOrderByLogDatetimeDesc(LogType logType, org.springframework.data.domain.Pageable pageable);
	
}
