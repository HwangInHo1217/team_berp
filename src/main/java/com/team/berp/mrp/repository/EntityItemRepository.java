package com.team.berp.mrp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Item;
import com.team.berp.domain.Mrp;

// JpaRepository<엔티티, PK 타입>을 상속받아, DB 접근 기본 기능 자동 제공
public interface EntityItemRepository extends JpaRepository<Item, Long> {
    // JpaRepository를 상속하면 별도 코드 없이 기본 CRUD 메서드 자동 제공
    // 예: findAll(), findById(), save(), deleteById() 등

    // 추가로, 필요한 경우 쿼리 메서드 정의 가능 (예: findByItemName(String name))
	
}
