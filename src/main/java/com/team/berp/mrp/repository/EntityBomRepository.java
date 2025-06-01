package com.team.berp.mrp.repository;

import com.team.berp.domain.Bom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntityBomRepository extends JpaRepository<Bom, Long> {
    /**
     * 상위 품목 코드(item.code)에 해당하는 BOM 투입 자재 리스트 조회
     */
    List<Bom> findByParentItemCode(String itemCode);
}
