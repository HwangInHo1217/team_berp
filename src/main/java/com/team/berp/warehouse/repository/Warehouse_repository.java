package com.team.berp.warehouse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.team.berp.domain.Warehouse;
import com.team.berp.domain.WarehouseType;


@Repository
public interface Warehouse_repository extends JpaRepository<Warehouse, Long> { // PK 타입 Integer 확인!
	
    // ==============================
    // 🔹 일반 조회
    // ==============================
	
    List<Warehouse> findByUseYn(String useYn);
    
    // Warehouse 엔티티의 PK 필드명이 'id' 이므로 OrderByIdDesc가 맞음
    List<Warehouse> findAllByOrderByIdDesc(); // 수정: WarehouseId -> Id
    
    Optional<Warehouse> findByWarehouseCode(String warehouseCode);
    
    // Warehouse 엔티티의 PK 필드명이 'id' 이므로 이 메소드들은 문제 없음
    Page<Warehouse> findAllByOrderByIdDesc(Pageable pageable); // 수정: WarehouseId -> Id
    Page<Warehouse> findByUseYnOrderByIdDesc(String useYn, Pageable pageable); // 수정: WarehouseId -> Id
    Page<Warehouse> findByWarehouseCodeContainingOrderByIdDesc(String keyword, Pageable pageable); // 수정: WarehouseId -> Id
    Page<Warehouse> findByWarehouseNameContainingOrderByIdDesc(String keyword, Pageable pageable); // 수정: WarehouseId -> Id
    
    // 여기! 이 메소드가 에러 로그에 언급된 것과 유사한 이름이야.
    // Warehouse 엔티티의 PK가 'id'이므로 'OrderByIdDesc'가 되어야 해.
    // 네가 올린 코드에는 이미 이렇게 되어 있네! 아주 좋아!
    Page<Warehouse> findByWarehouseCodeContainingAndUseYnOrderByIdDesc(String keyword, String useYn, Pageable pageable);
    
    // 이 메소드도 PK 필드명 'id'에 맞게 수정
    Page<Warehouse> findByWarehouseNameContainingAndUseYnOrderByIdDesc(String keyword, String useYn, Pageable pageable); // 수정: WarehouseId -> Id
    
    // ==============================
    // 🔹 코드 생성 관련
    // ==============================
    @Query("SELECT MAX(CAST(SUBSTRING(w.warehouseCode, 5) AS integer)) FROM Warehouse w WHERE w.warehouseType = :type AND w.warehouseCode LIKE :prefix")
    Integer findMaxCodeNumberByType(@Param("type") WarehouseType type, @Param("prefix") String prefix);
    
    @Query("SELECT CAST(SUBSTRING(w.warehouseCode, 5) AS integer) FROM Warehouse w WHERE w.warehouseType = :type AND w.warehouseCode LIKE :prefix ORDER BY CAST(SUBSTRING(w.warehouseCode, 5) AS integer)")
    List<Integer> findAllCodeNumbersByType(@Param("type") WarehouseType type, @Param("prefix") String prefix);
    
    // ========== 기존 비페이징 메소드 (호환성 유지) ==========
    // 여기도 PK 필드명 'id'에 맞게 수정
    List<Warehouse> findByUseYnOrderByIdDesc(String useYn); // 수정: WarehouseId -> Id
    List<Warehouse> findByWarehouseCodeContainingOrderByIdDesc(String keyword); // 수정: WarehouseId -> Id
    List<Warehouse> findByWarehouseNameContainingOrderByIdDesc(String keyword); // 수정: WarehouseId -> Id
    List<Warehouse> findByWarehouseCodeContainingAndUseYnOrderByIdDesc(String keyword, String useYn); // 여기도 'OrderByIdDesc'
    List<Warehouse> findByWarehouseNameContainingAndUseYnOrderByIdDesc(String keyword, String useYn); // 여기도 'OrderByIdDesc'
}