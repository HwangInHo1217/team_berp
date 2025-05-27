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
           "JOIN s.whs w " +
           "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Stock> searchByItem(@Param("keyword") String keyword, Pageable pageable);

    // 🔍 창고명 or 창고코드로 검색
    @Query("SELECT s FROM Stock s " +
           "JOIN s.item i " +
           "JOIN s.whs w " +
           "WHERE LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Stock> searchByWarehouse(@Param("keyword") String keyword, Pageable pageable);

    // 🔍 통합 검색 (품목명, 코드 + 창고명, 코드)
    @Query("SELECT s FROM Stock s " +
           "JOIN s.item i " +
           "JOIN s.whs w " +
           "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Stock> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 🔧 품목+창고 엔티티로 재고 조회
    Optional<Stock> findByItemAndWhs(Item item, Warehouse whs);

    // 🔧 창고명으로 검색 (페이징 지원)
    @Query("SELECT s FROM Stock s WHERE s.whs.warehouseName = :whsName")
    Page<Stock> findByWhsName(@Param("whsName") String whsName, Pageable pageable);

    // 🔧 특정 품목에 대한 모든 재고
    List<Stock> findByItem(Item item);

    // 🔧 특정 창고의 모든 재고
    List<Stock> findByWhs(Warehouse whs);

    // 🔧 로트번호로 검색
    List<Stock> findByLotNum(String lotNum);
}