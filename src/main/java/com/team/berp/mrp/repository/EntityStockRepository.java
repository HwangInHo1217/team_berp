package com.team.berp.mrp.repository;

import com.team.berp.domain.Stock;
import com.team.berp.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EntityStockRepository extends JpaRepository<Stock, Integer> {
    // 특정 품목의 재고 전체 조회
    List<Stock> findByItem(Item item);
}
