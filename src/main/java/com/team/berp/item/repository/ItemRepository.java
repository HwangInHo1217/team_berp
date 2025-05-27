package com.team.berp.item.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
@Repository
public interface ItemRepository extends JpaRepository<Item, Long>{
    // code가 "MAT%" 또는 "PRD%" 형태 중 가장 큰 값 조회
   
    //String findLastCodeByPrefix(@Param("prefix") String prefix);
	// 가장 마지막 code 조회 (예: "MAT%" 또는 "PRD%")
	//@Query("SELECT i.code FROM Item i WHERE i.code LIKE :prefix ORDER BY i.code DESC")
	//public String findTop1ByCodeStartingWithOrderByCodeDesc(String prefix);
	// ✅ 정확한 형식
	@Query("SELECT i.code FROM Item i WHERE i.code LIKE CONCAT(:prefix, '%') ORDER BY i.code DESC")
	List<String> findCodesByPrefix(@Param("prefix") String prefix);

    List<Item> findByNameContaining(String keyword);
    List<Item> findByCodeContaining(String keyword);
    
 // 품목 이름에 keyword가 포함된 데이터를 페이징해서 조회
    Page <Item> findByNameContaining(String keyword, Pageable pageable);
    
 // 품목 코드에 keyword가 포함된 데이터를 페이징해서 조회
    Page<Item> findByCodeContaining(String keyword, Pageable pageable);
    List<Item> findByType(ItemType type); // ✅ ENUM 타입으로 받기
    
    // ✅ 1. 이름 검색 + 제품
    Page<Item> findByNameContainingAndType(String name, String type, Pageable pageable);

    // ✅ 2. 코드 검색 + 제품
    Page<Item> findByCodeContainingAndType(String code, String type, Pageable pageable);

    // ✅ 3. 이름 검색 + 제품 + 사용여부
    Page<Item> findByNameContainingAndTypeAndUse(String name, String type, String use, Pageable pageable);

    // ✅ 4. 코드 검색 + 제품 + 사용여부
    Page<Item> findByCodeContainingAndTypeAndUse(String code, String type, String use, Pageable pageable);

    // ✅ 5. 전체 제품 목록
    Page<Item> findByType(String type, Pageable pageable);

    // ✅ 6. 전체 제품 목록 + 사용여부 (fallback용)
    Page<Item> findByTypeAndUse(String type, String use, Pageable pageable);
    
    Page<Item> findByNameContainingAndType(String name, ItemType type, Pageable pageable);
    Page<Item> findByNameContainingAndTypeAndUse(String name, ItemType type, String use, Pageable pageable);

    Page<Item> findByCodeContainingAndType(String code, ItemType type, Pageable pageable);
    Page<Item> findByCodeContainingAndTypeAndUse(String code, ItemType type, String use, Pageable pageable);

    Page<Item> findByType(ItemType type, Pageable pageable);
    
 // ✅ 특정 품목 유형 + 사용 여부로 필터링하여 페이징 처리
    Page<Item> findByTypeAndUse(ItemType type, String use, Pageable pageable);
 // JpaRepository<Item, Long> 에서 자동 제공됨
    Page<Item> findAll(Pageable pageable);

}
