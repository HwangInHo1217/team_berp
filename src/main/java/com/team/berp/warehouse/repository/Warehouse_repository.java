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
import com.team.berp.domain.Warehouse.WarehouseType;

/**
 * Warehouse_repository
 *
 * 창고(Warehouse) 엔티티에 대한 데이터 접근 Repository 인터페이스.
 * - JpaRepository 상속을 통해 기본 CRUD 기능 제공
 * - 페이징, 필터링, 키워드 검색, 코드 자동 생성 등을 위한 커스텀 메서드 정의
 */
@Repository
public interface Warehouse_repository extends JpaRepository<Warehouse, Integer> {
	
    // ==============================
    // 🔹 일반 조회
    // ==============================
	

    /**
     * 사용여부(Y/N)에 따른 전체 목록 조회
     */
    List<Warehouse> findByUseYn(String useYn);
    
    /**
     * ID 역순 전체 목록 조회 (기본 정렬용)
     */
    List<Warehouse> findAllByOrderByWarehouseIdDesc();
    
    /**
     * 창고 코드로 단건 조회
     */
    Optional<Warehouse> findByWarehouseCode(String warehouseCode);
    
    /**
     * 창고 유형별 개수 세기
     */
    long countByWarehouseType(WarehouseType warehouseType);
    
    // ==============================
    // 🔹 코드 생성 관련
    // ==============================

    /**
     * 주어진 유형(type)의 코드 중 가장 큰 숫자를 가져옴
     * 예: RWWH0001, RWWH0002 → 최대 숫자는 2
     */
    @Query("SELECT MAX(CAST(SUBSTRING(w.warehouseCode, 5) AS integer)) FROM Warehouse w WHERE w.warehouseType = :type AND w.warehouseCode LIKE :prefix")
    Integer findMaxCodeNumberByType(@Param("type") WarehouseType type, @Param("prefix") String prefix);
    
    /**
     * 주어진 유형의 모든 코드 숫자만 뽑아옴
     * - 누락된 번호 찾을 때 유용 (ex. 1, 2, 4 → 3이 비었는지 판단)
     */
    @Query("SELECT CAST(SUBSTRING(w.warehouseCode, 5) AS integer) FROM Warehouse w WHERE w.warehouseType = :type AND w.warehouseCode LIKE :prefix ORDER BY CAST(SUBSTRING(w.warehouseCode, 5) AS integer)")
    List<Integer> findAllCodeNumbersByType(@Param("type") WarehouseType type, @Param("prefix") String prefix);
    
    // ==============================
    // 🔹 페이징 조회
    // ==============================
    
    /**
     * 전체 목록 페이징 (ID 내림차순)
     */
    Page<Warehouse> findAllByOrderByWarehouseIdDesc(Pageable pageable);
    
    /**
     * 사용여부 필터 페이징
     */
    Page<Warehouse> findByUseYnOrderByWarehouseIdDesc(String useYn, Pageable pageable);
    
    /**
     * 코드로 검색 (페이징)
     */
    Page<Warehouse> findByWarehouseCodeContainingOrderByWarehouseIdDesc(String keyword, Pageable pageable);
    
    /**
     * 이름으로 검색 (페이징)
     */
    Page<Warehouse> findByWarehouseNameContainingOrderByWarehouseIdDesc(String keyword, Pageable pageable);
    
    /**
     * 코드로 검색 + 사용여부 필터 (페이징)
     */
    Page<Warehouse> findByWarehouseCodeContainingAndUseYnOrderByWarehouseIdDesc(String keyword, String useYn, Pageable pageable);
    
    /**
     * 이름으로 검색 + 사용여부 필터 (페이징)
     */
    Page<Warehouse> findByWarehouseNameContainingAndUseYnOrderByWarehouseIdDesc(String keyword, String useYn, Pageable pageable);
    
    // ========== 기존 비페이징 메소드 (호환성 유지) ==========
    // ==============================
    // 🔹 비페이징 검색 (구버전 JS 호환용)
    // ==============================    
    List<Warehouse> findByUseYnOrderByWarehouseIdDesc(String useYn);
    List<Warehouse> findByWarehouseCodeContainingOrderByWarehouseIdDesc(String keyword);
    List<Warehouse> findByWarehouseNameContainingOrderByWarehouseIdDesc(String keyword);
    List<Warehouse> findByWarehouseCodeContainingAndUseYnOrderByWarehouseIdDesc(String keyword, String useYn);
    List<Warehouse> findByWarehouseNameContainingAndUseYnOrderByWarehouseIdDesc(String keyword, String useYn);
}