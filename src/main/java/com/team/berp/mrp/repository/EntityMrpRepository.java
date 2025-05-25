package com.team.berp.mrp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.team.berp.domain.Mrp;

public interface EntityMrpRepository extends JpaRepository<Mrp, Integer> {

    // itemName 기준 정렬 (DESC/ASC 선택 가능, 기본 DESC)
    @Query(
        "SELECT m FROM Mrp m " +
        "JOIN m.item i " +
        "ORDER BY i.itemName DESC"
    )
    Page<Mrp> findAllOrderByItemNameDesc(Pageable pageable);

    // ASC 예시
    @Query(
        "SELECT m FROM Mrp m " +
        "JOIN m.item i " +
        "ORDER BY i.itemName ASC"
    )
    Page<Mrp> findAllOrderByItemNameAsc(Pageable pageable);
    
    Page<Mrp> findAll(Pageable pageable);
}

