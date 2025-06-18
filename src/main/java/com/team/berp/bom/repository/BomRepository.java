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
import com.team.berp.domain.BomVersion;
import com.team.berp.domain.Item;

public interface BomRepository extends JpaRepository<Bom, Integer> {

	@Query("SELECT b FROM Bom b JOIN FETCH b.childItem WHERE b.parentItem = :parent")
	List<Bom> findByParentItem(@Param("parent") Item parent);
	
	// ✅ 3. Repository 수정
	List<Bom> findByBomVersion(BomVersion version);

	@Query("SELECT DISTINCT b.parentItem FROM Bom b WHERE b.parentItem.type = 'product'")
	List<Item> findDistinctParentItems();

	List<Bom> findByParentItemId(Long parentId);
	List<Bom> findByBomVersion_Id(Long versionId);

	@Modifying
	@Query("DELETE FROM Bom b WHERE b.parentItem.id = :parentItemId")
	void deleteByParentItemId(@Param("parentItemId") Long parentItemId);
	
	/*
	 * @Query(""" SELECT new com.team.berp.bom.dto.BomProductItemDTO( b.bomId,
	 * b.parentItem.code, b.parentItem.name, b.parentItem.spec, b.parentItem.unit,
	 * b.parentItem.use, b.parentItem.type ) FROM Bom b WHERE b.parentItem.name LIKE
	 * %:keyword% OR b.parentItem.code LIKE %:keyword%
	 * 
	 * """) Page<BomProductItemDTO> findPagedByKeyword(@Param("keyword") String
	 * keyword, Pageable pageable);
	 */
	@Query("""
			    SELECT DISTINCT new com.team.berp.bom.dto.BomProductItemDTO(
			        b.parentItem.id,
			        b.parentItem.code,
			        b.parentItem.name,
			        b.parentItem.spec,
			        b.parentItem.unit,
			        b.parentItem.use,
			        b.parentItem.type
			    )
			    FROM Bom b
			    WHERE (:keyword IS NULL OR :keyword = '' OR
			          b.parentItem.name LIKE %:keyword% OR
			          b.parentItem.code LIKE %:keyword%)
			""")
	Page<BomProductItemDTO> findPagedParentItemsByKeyword(@Param("keyword") String keyword, Pageable pageable);

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
	
	@Query("""
		    SELECT new com.team.berp.bom.dto.BomProductItemDTO(
		        i.id, i.code, i.name, i.spec, i.unit, i.use, i.type)
		    FROM Item i
		    WHERE ( 
		        (:searchField = 'name' AND (:keyword IS NULL OR i.name LIKE %:keyword%)) OR
		        (:searchField = 'code' AND (:keyword IS NULL OR i.code LIKE %:keyword%)) OR
		        (:searchField IS NULL AND (:keyword IS NULL OR i.name LIKE %:keyword% OR i.code LIKE %:keyword%))
		    )
		    AND (:useYn IS NULL OR i.use = :useYn)
		    AND i.type = 'product'
		    """)
		Page<BomProductItemDTO> searchBomProducts(
		    @Param("searchField") String searchField,
		    @Param("keyword") String keyword,
		    @Param("useYn") String useYn,
		    Pageable pageable);



}
