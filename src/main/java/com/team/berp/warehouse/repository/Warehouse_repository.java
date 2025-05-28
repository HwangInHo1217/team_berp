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
public interface Warehouse_repository extends JpaRepository<Warehouse, Long> {
	
    // ==============================
    // 🔹 일반 조회
    // ==============================
	
    List<Warehouse> findByUseYn(String useYn);
    
    List<Warehouse> findAllByOrderByIdDesc();
    
    Optional<Warehouse> findByWarehouseCode(String warehouseCode);
    
    Page<Warehouse> findAllByOrderByIdDesc(Pageable pageable);
    Page<Warehouse> findByUseYnOrderByIdDesc(String useYn, Pageable pageable);
    Page<Warehouse> findByWarehouseCodeContainingOrderByIdDesc(String keyword, Pageable pageable);
    Page<Warehouse> findByWarehouseNameContainingOrderByIdDesc(String keyword, Pageable pageable);
    Page<Warehouse> findByWarehouseCodeContainingAndUseYnOrderByIdDesc(String keyword, String useYn, Pageable pageable);
    Page<Warehouse> findByWarehouseNameContainingAndUseYnOrderByIdDesc(String keyword, String useYn, Pageable pageable);
    
    // ==============================
    // 🔹 코드 생성 관련
    // ==============================
    @Query("SELECT MAX(CAST(SUBSTRING(w.warehouseCode, 5) AS integer)) FROM Warehouse w WHERE w.warehouseType = :type AND w.warehouseCode LIKE :prefix")
    Integer findMaxCodeNumberByType(@Param("type") WarehouseType type, @Param("prefix") String prefix);
    
    @Query("SELECT CAST(SUBSTRING(w.warehouseCode, 5) AS integer) FROM Warehouse w WHERE w.warehouseType = :type AND w.warehouseCode LIKE :prefix ORDER BY CAST(SUBSTRING(w.warehouseCode, 5) AS integer)")
    List<Integer> findAllCodeNumbersByType(@Param("type") WarehouseType type, @Param("prefix") String prefix);
    
    // ========== 기존 비페이징 메소드 (호환성 유지) ==========
    List<Warehouse> findByUseYnOrderByIdDesc(String useYn);
    List<Warehouse> findByWarehouseCodeContainingOrderByIdDesc(String keyword);
    List<Warehouse> findByWarehouseNameContainingOrderByIdDesc(String keyword);
    List<Warehouse> findByWarehouseCodeContainingAndUseYnOrderByIdDesc(String keyword, String useYn);
    List<Warehouse> findByWarehouseNameContainingAndUseYnOrderByIdDesc(String keyword, String useYn);
}