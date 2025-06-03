// File: /Team_BERP/src/main/java/com/team/berp/mrp/repository/WarehouseRepository.java
package com.team.berp.mrp.repository;

import com.team.berp.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Mrp_WarehouseRepository extends JpaRepository<Warehouse, Long> {
    // 별도 커스텀 쿼리는 없으며, 기본 제공 메소드만 사용합니다.
}
