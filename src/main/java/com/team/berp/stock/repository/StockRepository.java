package com.team.berp.stock.repository;

import com.team.berp.domain.Stock;
import com.team.berp.domain.Item;
import com.team.berp.domain.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//=============================================================================
//🗄️ StockRepository.java - 데이터 접근 계층
//=============================================================================

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
 
 /**
  * 🔍 품목명/코드 검색
  */
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
 Page<Stock> searchByItem(@Param("keyword") String keyword, Pageable pageable);
 
 /**
  * 🔍 창고명/코드 검색
  */
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
 Page<Stock> searchByWarehouse(@Param("keyword") String keyword, Pageable pageable);
 
 /**
  * 🔍 통합 키워드 검색 (가장 많이 사용)
  */
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
 Page<Stock> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
 
 /**
  * 🔧 품목+창고로 재고 조회 (입출고시 사용)
  */
 Optional<Stock> findByItemAndWarehouse(Item item, Warehouse warehouse);
 
 /**
  * 🔧 창고코드로 검색
  */
 @Query("SELECT s FROM Stock s WHERE s.warehouse.warehouseCode = :whsCode")
 Page<Stock> findByWarehouseCode(@Param("whsCode") String whsCode, Pageable pageable);
 
 /**
  * 🔧 창고ID로 검색
  */
 Page<Stock> findByWarehouse_Id(Long warehouseId, Pageable pageable);
 
 /**
  * 🔧 품목유형별 검색
  */
 @Query("SELECT s FROM Stock s WHERE s.item.type = :itemType")
 Page<Stock> findByItemType(@Param("itemType") com.team.berp.domain.ItemType itemType, Pageable pageable);
 
 /**
  * 🔧 품목유형+창고 조합
  */
 @Query("SELECT s FROM Stock s WHERE s.item.type = :itemType AND s.warehouse.warehouseCode = :whsCode")
 Page<Stock> findByItemTypeAndWarehouseCode(@Param("itemType") com.team.berp.domain.ItemType itemType, 
                                           @Param("whsCode") String whsCode, Pageable pageable);
 
 /**
  * 📊 재고 상태별 검색
  */
 @Query("SELECT s FROM Stock s WHERE s.quantity = 0")
 Page<Stock> findOutOfStock(Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.quantity > 0")
 Page<Stock> findInStock(Pageable pageable);
 
 /**
  * 🔍 기타 검색
  */
 List<Stock> findByItem(Item item);
 List<Stock> findByWarehouse(Warehouse warehouse);
 List<Stock> findByLotNumber(String lotNumber);
}
