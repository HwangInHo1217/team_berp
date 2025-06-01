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
 
 // === 기존 메서드들 ===
 
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
 Page<Stock> searchByItem(@Param("keyword") String keyword, Pageable pageable);
 
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
 Page<Stock> searchByWarehouse(@Param("keyword") String keyword, Pageable pageable);
 
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
 Page<Stock> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
 
 Optional<Stock> findByItemAndWarehouse(Item item, Warehouse warehouse);
 
 @Query("SELECT s FROM Stock s WHERE s.warehouse.warehouseCode = :whsCode")
 Page<Stock> findByWarehouseCode(@Param("whsCode") String whsCode, Pageable pageable);
 
 Page<Stock> findByWarehouse_Id(Long warehouseId, Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.item.type = :itemType")
 Page<Stock> findByItemType(@Param("itemType") com.team.berp.domain.ItemType itemType, Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.item.type = :itemType AND s.warehouse.warehouseCode = :whsCode")
 Page<Stock> findByItemTypeAndWarehouseCode(@Param("itemType") com.team.berp.domain.ItemType itemType, 
                                           @Param("whsCode") String whsCode, Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.quantity = 0")
 Page<Stock> findOutOfStock(Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.quantity > 0")
 Page<Stock> findInStock(Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.quantity < 10 AND s.quantity > 0")
 Page<Stock> findBelowSafety(Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND w.warehouseCode = :whsCode")
 Page<Stock> searchByKeywordAndWarehouse(@Param("keyword") String keyword, 
                                        @Param("whsCode") String whsCode, 
                                        Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND i.type = :itemType")
 Page<Stock> searchByKeywordAndItemType(@Param("keyword") String keyword, 
                                       @Param("itemType") com.team.berp.domain.ItemType itemType, 
                                       Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND w.warehouseCode = :whsCode " +
        "  AND i.type = :itemType")
 Page<Stock> searchByKeywordAndWarehouseAndItemType(@Param("keyword") String keyword,
                                                   @Param("whsCode") String whsCode,
                                                   @Param("itemType") com.team.berp.domain.ItemType itemType,
                                                   Pageable pageable);

 // === ⭐ 새로 추가되는 재고상태 조합 메서드들 ⭐ ===
 
 /**
  * 🔧 키워드 + 재고상태 조합
  */
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> searchByKeywordAndStockStatus(@Param("keyword") String keyword,
                                          @Param("stockStatus") String stockStatus,
                                          Pageable pageable);

 /**
  * 🔧 창고 + 재고상태 조합
  */
 @Query("SELECT s FROM Stock s " +
        "WHERE s.warehouse.warehouseCode = :whsCode " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> findByWarehouseCodeAndStockStatus(@Param("whsCode") String whsCode,
                                              @Param("stockStatus") String stockStatus,
                                              Pageable pageable);

 /**
  * 🔧 품목유형 + 재고상태 조합
  */
 @Query("SELECT s FROM Stock s " +
        "WHERE s.item.type = :itemType " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> findByItemTypeAndStockStatus(@Param("itemType") com.team.berp.domain.ItemType itemType,
                                         @Param("stockStatus") String stockStatus,
                                         Pageable pageable);

 /**
  * 🔧 키워드 + 창고 + 재고상태 조합  
  */
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND w.warehouseCode = :whsCode " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> searchByKeywordAndWarehouseAndStockStatus(@Param("keyword") String keyword,
                                                      @Param("whsCode") String whsCode,
                                                      @Param("stockStatus") String stockStatus,
                                                      Pageable pageable);

 /**
  * 🔧 키워드 + 품목유형 + 재고상태 조합
  */
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND i.type = :itemType " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> searchByKeywordAndItemTypeAndStockStatus(@Param("keyword") String keyword,
                                                     @Param("itemType") com.team.berp.domain.ItemType itemType,
                                                     @Param("stockStatus") String stockStatus,
                                                     Pageable pageable);

 /**
  * 🔧 창고 + 품목유형 + 재고상태 조합 (기존 메서드 수정)
  */
 @Query("SELECT s FROM Stock s " +
        "WHERE s.item.type = :itemType " +
        "  AND s.warehouse.warehouseCode = :whsCode " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> findByItemTypeAndWarehouseCodeAndStockStatus(@Param("itemType") com.team.berp.domain.ItemType itemType,
                                                         @Param("whsCode") String whsCode,
                                                         @Param("stockStatus") String stockStatus,
                                                         Pageable pageable);

 /**
  * 🔧 키워드 + 창고 + 품목유형 + 재고상태 조합 (최종 복합 조건)
  */
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND w.warehouseCode = :whsCode " +
        "  AND i.type = :itemType " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> searchByAllConditions(@Param("keyword") String keyword,
                                  @Param("whsCode") String whsCode,
                                  @Param("itemType") com.team.berp.domain.ItemType itemType,
                                  @Param("stockStatus") String stockStatus,
                                  Pageable pageable);

 

/**
* 재고 수량별 카운트 조회 (재고 없는 품목 수)
*/
long countByQuantity(Integer quantity);

/**
* 재고 수량 범위별 카운트 조회 (안전재고 미달용)
* 예: 1개 이상 9개 이하 = 안전재고 미달
*/
long countByQuantityBetween(Integer minQty, Integer maxQty);

/**
* 창고별 재고 현황 통계 (필요시 사용)
*/
@Query("SELECT w.warehouseName, COUNT(s) FROM Stock s " +
     "JOIN s.warehouse w " +
     "GROUP BY w.warehouseName")
List<Object[]> getStockCountByWarehouse();

/**
* 전체 재고 아이템 수 (중복 제거)
*/
@Query("SELECT COUNT(DISTINCT s.item.id) FROM Stock s")
long countDistinctItems();
 
 
 // === 기타 메서드들 ===
 
 List<Stock> findByItem(Item item);
 List<Stock> findByWarehouse(Warehouse warehouse);
 List<Stock> findByLotNumber(String lotNumber);
}