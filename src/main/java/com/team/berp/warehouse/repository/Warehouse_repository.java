package com.team.berp.warehouse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.team.berp.domain.Warehouse;
import com.team.berp.domain.Warehouse.WarehouseType;

/**
 * 창고 데이터 접근 Repository
 * 창고 엔티티에 대한 데이터베이스 접근 메소드를 제공합니다.
 */
@Repository
public interface Warehouse_repository extends JpaRepository<Warehouse, Integer> {

    // 기본 조회
    List<Warehouse> findByUseYn(String useYn);
    List<Warehouse> findAllByOrderByWarehouseIdDesc();
    Optional<Warehouse> findByWarehouseCode(String warehouseCode);
    
    // 카운트
    long countByWarehouseType(WarehouseType warehouseType);
    
    // 사용여부 필터링
    List<Warehouse> findByUseYnOrderByWarehouseIdDesc(String useYn);
    
    // 전체 데이터 대상 검색
    List<Warehouse> findByWarehouseCodeContainingOrderByWarehouseIdDesc(String keyword);
    List<Warehouse> findByWarehouseNameContainingOrderByWarehouseIdDesc(String keyword);
    
    // 기존 메소드들 (호환성 유지)
    List<Warehouse> findByWarehouseCodeContainingAndUseYnOrderByWarehouseIdDesc(String keyword, String useYn);
    List<Warehouse> findByWarehouseNameContainingAndUseYnOrderByWarehouseIdDesc(String keyword, String useYn);
}