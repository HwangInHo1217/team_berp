package com.team.berp.bom.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team.berp.bom.dto.BomProductItemDTO;
import com.team.berp.domain.Bom;
import com.team.berp.domain.Item;

public interface BomRepository extends JpaRepository<Bom, Integer> {

	List<Bom> findByParentItem(Item parent);
	
	@Query("SELECT DISTINCT b.parentItem FROM Bom b WHERE b.parentItem.type = 'product'")
	List<Item> findDistinctParentItems();

	List<Bom> findByParentItemId(Long parentId);
	@Modifying
	@Query("DELETE FROM Bom b WHERE b.parentItem.id = :parentItemId")
	void deleteByParentItemId(@Param("parentItemId") Long parentItemId);
	
	@Query("""
		    SELECT new com.team.berp.bom.dto.BomProductItemDTO(
		        b.bomId,
		        b.parentItem.code,
		        b.parentItem.name,
		        b.parentItem.spec,
		        b.parentItem.unit,
		        b.parentItem.use,
		        b.parentItem.type
		    )
		    FROM Bom b
		    WHERE b.parentItem.name LIKE %:keyword%
		       OR b.parentItem.code LIKE %:keyword%
		    
		""")
		Page<BomProductItemDTO> findPagedByKeyword(@Param("keyword") String keyword, Pageable pageable);

		
	@Query("""
		    SELECT new com.team.berp.bom.dto.BomProductItemDTO(
		        b.bomId,
		        b.parentItem.code,
		        b.parentItem.name,
		        b.parentItem.spec,
		        b.parentItem.unit,
		        b.parentItem.use,
		        b.parentItem.type
		    )
		    FROM Bom b
		    WHERE b.parentItem.type = com.team.berp.domain.ItemType.product
		    ORDER BY b.parentItem.code
		""")
		Page<BomProductItemDTO> findAllParentItems(Pageable pageable);



	
}
