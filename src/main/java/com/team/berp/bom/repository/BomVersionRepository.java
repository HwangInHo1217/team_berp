package com.team.berp.bom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team.berp.domain.BomVersion;
import com.team.berp.domain.Item;

public interface BomVersionRepository extends JpaRepository<BomVersion, Long> {
	// parentItem 별 버전 목록 조회
	List<BomVersion> findByParentItem(Item parent);
	
	/**
     *  부모 Item ID(parent_item_id)를 기준으로 가장 숫자가 큰 버전 하나만 가져온다.
     *
     *  version_code 컬럼이 "V1", "V2", "V10" 같은 형식이라고 가정.
     *  SUBSTRING(version_code, 2)로 "1", "2", "10" 부분을 뽑아내고,
     *  CAST(... AS UNSIGNED)로 정수로 변환한 뒤 DESC 정렬하여 LIMIT 1.
     *
     *  nativeQuery=true 로 순수 SQL을 그대로 사용했습니다.
     */
    @Query(value = 
        "SELECT *\n" +
        "  FROM bom_version\n" +
        " WHERE parent_item_id = :parentId\n" +
        " ORDER BY CAST(SUBSTRING(version_code, 2) AS UNSIGNED) DESC\n" +
        " LIMIT 1",
        nativeQuery = true
    )
    Optional<BomVersion> findLatestVersionByParent(@Param("parentId") Long parentId);

}
