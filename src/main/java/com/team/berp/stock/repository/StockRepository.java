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

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    // 🔍 품목명 or 품목코드로 검색
    @Query("SELECT s FROM Stock s " +
           "JOIN s.item i " +
           "JOIN s.warehouse w " +
           "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Stock> searchByItem(@Param("keyword") String keyword, Pageable pageable);
    
    // 🔍 창고명 or 창고코드로 검색
    @Query("SELECT s FROM Stock s " +
           "JOIN s.item i " +
           "JOIN s.warehouse w " +
           "WHERE LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Stock> searchByWarehouse(@Param("keyword") String keyword, Pageable pageable);
    
    // 🔍 통합 검색 (품목명, 코드 + 창고명, 코드)
    @Query("SELECT s FROM Stock s " +
           "JOIN s.item i " +
           "JOIN s.warehouse w " +
           "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Stock> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 🔧 품목+창고 엔티티로 재고 조회
    Optional<Stock> findByItemAndWarehouse(Item item, Warehouse warehouse);
    
    // 🔧 창고 코드로 검색 (효율적)
    @Query("SELECT s FROM Stock s WHERE s.warehouse.warehouseCode = :whsCode")
    Page<Stock> findByWarehouseCode(@Param("whsCode") String whsCode, Pageable pageable);
    
    // 🔧 창고 ID로 검색 (가장 효율적)
    Page<Stock> findByWarehouse_Id(Integer warehouseId, Pageable pageable);
    
    // 🔧 품목 유형별 조회
    @Query("SELECT s FROM Stock s WHERE s.item.type = :itemType")
    Page<Stock> findByItemType(@Param("itemType") String itemType, Pageable pageable);
    
    // 🔧 품목 유형 + 창고별 조회
    @Query("SELECT s FROM Stock s WHERE s.item.type = :itemType AND s.warehouse.warehouseCode = :whsCode")
    Page<Stock> findByItemTypeAndWarehouseCode(@Param("itemType") String itemType, @Param("whsCode") String whsCode, Pageable pageable);
    
    // 🔧 재고 상태별 조회
    @Query("SELECT s FROM Stock s WHERE s.quantity = 0")
    Page<Stock> findOutOfStock(Pageable pageable);
    
    @Query("SELECT s FROM Stock s WHERE s.quantity > 0")
    Page<Stock> findInStock(Pageable pageable);
    
    // 🔧 특정 품목에 대한 모든 재고
    List<Stock> findByItem(Item item);
    
    // 🔧 특정 창고의 모든 재고
    List<Stock> findByWarehouse(Warehouse warehouse);
    
    // 🔧 로트번호로 검색
    List<Stock> findByLotNumber(String lotNumber);
}