// 3. com/team/berp/order/repository/OrderItemRepository.java
package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.team.berp.domain.Item;

/**
 * Repository for Item entities (주문 품목 관리).
 */
@Repository
public interface Order_ItemRepository extends JpaRepository<Item, Long> {

    /**
     * 특정 품목명에 대한 단위를 조회합니다.
     * @param itemName 품목명
     * @return 단위 문자열
     */
    @Query("SELECT i.unit FROM Item i WHERE i.name = :name")
    String findUnitByItemName(@Param("name") String name);

    /**
     * 특정 품목코드에 대한 단가를 조회합니다.
     * @param itemCode 품목코드
     * @return 단가
     */
    @Query("SELECT i.unitPrice FROM Item i WHERE i.code = :code")
    Long findUnitPriceByItemCode(@Param("code") String code);
}