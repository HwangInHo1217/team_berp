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
 
 // === 기존 메서드들을 사용 중인 창고로 제한 ===
 
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE w.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
 Page<Stock> searchByItem(@Param("keyword") String keyword, Pageable pageable);
 
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE w.useYn = 'Y' AND " +
        "(LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
 Page<Stock> searchByWarehouse(@Param("keyword") String keyword, Pageable pageable);
 
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE w.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
 Page<Stock> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
 
 Optional<Stock> findByItemAndWarehouse(Item item, Warehouse warehouse);
 
 @Query("SELECT s FROM Stock s WHERE s.warehouse.warehouseCode = :whsCode AND s.warehouse.useYn = 'Y'")
 Page<Stock> findByWarehouseCode(@Param("whsCode") String whsCode, Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.warehouse.id = :warehouseId AND s.warehouse.useYn = 'Y'")
 Page<Stock> findByWarehouse_Id(@Param("warehouseId") Long warehouseId, Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.item.type = :itemType AND s.warehouse.useYn = 'Y'")
 Page<Stock> findByItemType(@Param("itemType") com.team.berp.domain.ItemType itemType, Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.item.type = :itemType AND s.warehouse.warehouseCode = :whsCode AND s.warehouse.useYn = 'Y'")
 Page<Stock> findByItemTypeAndWarehouseCode(@Param("itemType") com.team.berp.domain.ItemType itemType, 
                                           @Param("whsCode") String whsCode, Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.quantity = 0 AND s.warehouse.useYn = 'Y'")
 Page<Stock> findOutOfStock(Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.quantity > 0 AND s.warehouse.useYn = 'Y'")
 Page<Stock> findInStock(Pageable pageable);
 
 @Query("SELECT s FROM Stock s WHERE s.quantity < 10 AND s.quantity > 0 AND s.warehouse.useYn = 'Y'")
 Page<Stock> findBelowSafety(Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE w.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND w.warehouseCode = :whsCode")
 Page<Stock> searchByKeywordAndWarehouse(@Param("keyword") String keyword, 
                                        @Param("whsCode") String whsCode, 
                                        Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "WHERE s.warehouse.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND i.type = :itemType")
 Page<Stock> searchByKeywordAndItemType(@Param("keyword") String keyword, 
                                       @Param("itemType") com.team.berp.domain.ItemType itemType, 
                                       Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE w.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND w.warehouseCode = :whsCode " +
        "  AND i.type = :itemType")
 Page<Stock> searchByKeywordAndWarehouseAndItemType(@Param("keyword") String keyword,
                                                   @Param("whsCode") String whsCode,
                                                   @Param("itemType") com.team.berp.domain.ItemType itemType,
                                                   Pageable pageable);

 // === 재고상태 조합 메서드들에도 창고 사용여부 필터 추가 ===
 
 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE w.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "    OR LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> searchByKeywordAndStockStatus(@Param("keyword") String keyword,
                                          @Param("stockStatus") String stockStatus,
                                          Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "WHERE s.warehouse.warehouseCode = :whsCode AND s.warehouse.useYn = 'Y' " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> findByWarehouseCodeAndStockStatus(@Param("whsCode") String whsCode,
                                              @Param("stockStatus") String stockStatus,
                                              Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "WHERE s.item.type = :itemType AND s.warehouse.useYn = 'Y' " +
        "  AND (" +
        "    (:stockStatus = 'inStock' AND s.quantity > 0) OR " +
        "    (:stockStatus = 'outOfStock' AND s.quantity = 0) OR " +
        "    (:stockStatus = 'belowSafety' AND s.quantity < 10 AND s.quantity > 0)" +
        "  )")
 Page<Stock> findByItemTypeAndStockStatus(@Param("itemType") com.team.berp.domain.ItemType itemType,
                                         @Param("stockStatus") String stockStatus,
                                         Pageable pageable);

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE w.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
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

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "WHERE s.warehouse.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
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

 @Query("SELECT s FROM Stock s " +
        "WHERE s.item.type = :itemType AND s.warehouse.useYn = 'Y' " +
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

 @Query("SELECT s FROM Stock s " +
        "JOIN s.item i " +
        "JOIN s.warehouse w " +
        "WHERE w.useYn = 'Y' AND " +
        "(LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
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

 // === 전체 조회도 사용 중인 창고만 ===
 @Query("SELECT s FROM Stock s WHERE s.warehouse.useYn = 'Y'")
 Page<Stock> findAll(Pageable pageable);

 // === 통계 관련 메서드들도 사용 중인 창고만 ===
 @Query("SELECT COUNT(s) FROM Stock s WHERE s.warehouse.useYn = 'Y' AND s.quantity = :quantity")
 long countByQuantity(@Param("quantity") Integer quantity);

 @Query("SELECT COUNT(s) FROM Stock s WHERE s.warehouse.useYn = 'Y' AND s.quantity BETWEEN :minQty AND :maxQty")
 long countByQuantityBetween(@Param("minQty") Integer minQty, @Param("maxQty") Integer maxQty);

 @Query("SELECT COUNT(DISTINCT s.item.id) FROM Stock s WHERE s.warehouse.useYn = 'Y'")
 long countDistinctItems();

 // 기타 메서드들은 기존과 동일하게 유지
 List<Stock> findByItem(Item item);
 List<Stock> findByWarehouse(Warehouse warehouse);
 List<Stock> findByLotNumber(String lotNumber);
 
 long countByWarehouse_Id(Long warehouseId);
 long countByWarehouse_IdAndQuantity(Long warehouseId, Integer quantity);
 long countByWarehouse_IdAndQuantityBetween(Long warehouseId, Integer minQty, Integer maxQty);
 
 @Query("SELECT SUM(s.quantity) FROM Stock s WHERE s.warehouse.id = :warehouseId")
 Long sumQuantityByWarehouse_Id(@Param("warehouseId") Long warehouseId);

 @Query("SELECT s.item.type, COUNT(s), SUM(s.quantity) FROM Stock s " +
        "WHERE s.warehouse.id = :warehouseId " +
        "GROUP BY s.item.type")
 List<Object[]> getStockStatsByItemType(@Param("warehouseId") Long warehouseId);

 @Query("SELECT " +
        "SUM(CASE WHEN s.quantity = 0 THEN 1 ELSE 0 END) as outOfStock, " +
        "SUM(CASE WHEN s.quantity > 0 AND s.quantity < 10 THEN 1 ELSE 0 END) as belowSafety, " +
        "SUM(CASE WHEN s.quantity >= 10 THEN 1 ELSE 0 END) as normalStock " +
        "FROM Stock s WHERE s.warehouse.id = :warehouseId")
 Object[] getStockStatusStats(@Param("warehouseId") Long warehouseId);
}