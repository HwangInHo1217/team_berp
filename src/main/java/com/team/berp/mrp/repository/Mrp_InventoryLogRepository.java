// File: /Team_BERP/src/main/java/com/team/berp/mrp/repository/InventoryLogRepository.java
package com.team.berp.mrp.repository;

import com.team.berp.domain.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Mrp_InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    // 특정 품목(item_id)에 대한 로그를 일시 내림차순으로 조회
    List<InventoryLog> findByItem_IdOrderByLogDatetimeDesc(Long itemId);
}
