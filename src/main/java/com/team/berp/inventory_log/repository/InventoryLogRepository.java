package com.team.berp.inventory_log.repository;

import com.team.berp.domain.InventoryLog;
import com.team.berp.domain.LogType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

	// 품목+창고별 로그 조회
	Page<InventoryLog> findByItemIdAndWarehouseIdOrderByLogDatetimeDesc(Long itemId, Long warehouseId,
			Pageable pageable);

	// 특정 품목의 모든 로그
	Page<InventoryLog> findByItemIdOrderByLogDatetimeDesc(Long itemId, Pageable pageable);

	// 특정 창고의 모든 로그
	Page<InventoryLog> findByWarehouseIdOrderByLogDatetimeDesc(Long warehouseId, Pageable pageable);

	// 로그 타입별 조회
	Page<InventoryLog> findByLogTypeOrderByLogDatetimeDesc(LogType logType, Pageable pageable);

	// 기간별 로그 조회
	@Query("SELECT il FROM InventoryLog il " + "WHERE il.logDatetime BETWEEN :startDate AND :endDate "
			+ "ORDER BY il.logDatetime DESC")
	Page<InventoryLog> findByPeriod(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate, Pageable pageable);

	// 마지막 입고일 조회
	@Query("SELECT MAX(il.logDatetime) FROM InventoryLog il " + "WHERE il.item.id = :itemId "
			+ "AND il.warehouse.id = :warehouseId " + "AND il.logType = 'IN'")
	Optional<LocalDateTime> findLastInDate(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);

	// 마지막 출고일 조회
	@Query("SELECT MAX(il.logDatetime) FROM InventoryLog il " + "WHERE il.item.id = :itemId "
			+ "AND il.warehouse.id = :warehouseId " + "AND il.logType = 'OUT'")
	Optional<LocalDateTime> findLastOutDate(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);

	// 기간 내 입고 총량
	@Query("SELECT SUM(il.quantity) FROM InventoryLog il " + "WHERE il.item.id = :itemId "
			+ "AND il.warehouse.id = :warehouseId " + "AND il.logType = 'IN' "
			+ "AND il.logDatetime BETWEEN :startDate AND :endDate")
	Optional<Integer> sumInQuantityByPeriod(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId,
			@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

	// 기간 내 출고 총량
	@Query("SELECT SUM(il.quantity) FROM InventoryLog il " + "WHERE il.item.id = :itemId "
			+ "AND il.warehouse.id = :warehouseId " + "AND il.logType = 'OUT' "
			+ "AND il.logDatetime BETWEEN :startDate AND :endDate")
	Optional<Integer> sumOutQuantityByPeriod(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId,
			@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

	void deleteByOrderLineItem_OrderLineItemIdIn(List<Long> orderLineItemIds);

	boolean existsByOrderLineItem_OrderLineItemId(Long orderLineItemId);

}