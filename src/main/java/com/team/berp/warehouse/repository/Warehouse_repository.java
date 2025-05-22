package com.team.berp.warehouse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.warehouse.domain.Warehouse_entity;
import com.team.berp.warehouse.domain.WarehouseTypeEnum;
import org.springframework.data.domain.Sort;

@Repository
public interface Warehouse_repository extends JpaRepository<Warehouse_entity, Integer> {
    
    // 기본 CRUD 메소드는 JpaRepository로부터 상속받음
    
    // 사용 여부에 따른 창고 조회
    List<Warehouse_entity> findByUseYn(String useYn);
    List<Warehouse_entity> findAllByUseYn(String useYn);
    
    
    // 모든 창고 조회 (useYn 상관없이)
    List<Warehouse_entity> findAll();
    
    // 창고명 포함 검색
    List<Warehouse_entity> findByWarehouseNameContaining(String keyword);
    
    // 창고 타입별 조회
    List<Warehouse_entity> findByWarehouseType(WarehouseTypeEnum type);
    
    // 창고 ID 기준 내림차순 정렬
    List<Warehouse_entity> findAllByOrderByWarehouseIdDesc();
    
    // 창고명 기준 정렬
    List<Warehouse_entity> findAllByOrderByWarehouseNameAsc();
    
    // 타입과 사용여부를 함께 검색
    List<Warehouse_entity> findByWarehouseTypeAndUseYn(WarehouseTypeEnum type, String useYn);
    
    // 창고명 검색과 사용여부 함께 검색
    List<Warehouse_entity> findByWarehouseNameContainingAndUseYn(String keyword, String useYn);
    
    long countByWarehouseType(WarehouseTypeEnum warehouseType);

    
    // 직접 JPQL로 여러 조건 검색 (native=false 사용)
    @Query("SELECT w FROM Warehouse_entity w WHERE " +
    	       "(:keyword IS NULL OR :keyword = '' OR " +
    	       "LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
    	       "LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
    	       "(:type IS NULL OR w.warehouseType = :type) AND " +
    	       "(:useYn IS NULL OR :useYn = '' OR w.useYn = :useYn)")
    	List<Warehouse_entity> searchWarehouses(
    	        @Param("keyword") String keyword,
    	        @Param("type") WarehouseTypeEnum type,
    	        @Param("useYn") String useYn,
    	        Sort sort);
    
    	@Query("SELECT w FROM Warehouse_entity w WHERE (:useYn IS NULL OR w.useYn = :useYn)")
    	List<Warehouse_entity> findAllByUseYnOrAll(@Param("useYn") String useYn);
    
    	
    //수정할 때 중복체크시 필요할 것 같음
    Optional<Warehouse_entity> findByWarehouseCode(String warehouseCode);
	
    	
    	
    // 창고 정보 업데이트
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Warehouse_entity w SET " +
           "w.warehouseName = :name, " +
           "w.warehouseType = :type, " +
           "w.useYn = :useYn, " +
           "w.description = :description " +
           "WHERE w.warehouseId = :id")
    int updateWarehouse(
            @Param("id") Integer id,
            @Param("name") String name,
            @Param("type") WarehouseTypeEnum type,
            @Param("useYn") String useYn,
            @Param("description") String description);
    
    // Native SQL로 창고 상태 변경 (예시)
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE warehouse SET use_yn = :useYn WHERE warehouse_id = :id", nativeQuery = true)
    int changeWarehouseStatus(@Param("id") Integer id, @Param("useYn") String useYn);
}