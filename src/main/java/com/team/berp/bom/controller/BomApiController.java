package com.team.berp.bom.controller;


import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.bom.dto.AddBomRequestDTO;
import com.team.berp.bom.dto.BomEditResponseDTO;
import com.team.berp.bom.dto.BomListResponseDTO;
import com.team.berp.bom.dto.BomProductItemDTO;
import com.team.berp.bom.dto.BomTreeDTO;
import com.team.berp.bom.dto.BomVersionResponseDTO;
import com.team.berp.bom.dto.ItemSelectionDTO;
import com.team.berp.bom.dto.UpdateBomRequestDTO;
import com.team.berp.bom.service.BomService;
import com.team.berp.bom.service.BomVersionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bom") // ✅ 공통 prefix 추가
public class BomApiController {
	private final BomService bomService;
	private final BomVersionService bomVersionService;
	// ✅ 등록
    // 수정: 자동 생성된 versionId, versionCode를 함께 반환
    @PostMapping
    public ResponseEntity<?> registerBom(@RequestBody AddBomRequestDTO dto) {
        try {
            BomVersionResponseDTO respDto = bomService.registerBomWithVersion(dto);
            return ResponseEntity.ok(Map.of(
                "versionId", respDto.getId(),
                "versionCode", respDto.getVersionCode(),
                "useYn", respDto.getUseYn()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/tree/{parentId}")
    public ResponseEntity<BomTreeDTO> getBomTree(@PathVariable("parentId") Long parentId) {
        BomTreeDTO tree = bomService.getBomTreeByParent(parentId);
        return ResponseEntity.ok(tree);
    }


/*
	// ✅ 상세 조회
	@GetMapping("/version/{versionId}")
	public BomListViewResponse getBomByVersionId(@PathVariable("versionId") Long versionId) {
	    return bomService.getBomByVersionId(versionId);
	}*/
	// ✅ 1-1. BOM 버전 목록 조회
	@GetMapping("/versions/{parentId}")
	public List<BomVersionResponseDTO> getVersionsByParentId(@PathVariable("parentId") Long parentId) {
	    return bomService.getVersionsByParentId(parentId);
	}
	// ✅ 수정
	@PutMapping
	public ResponseEntity<Void> updateBom(@RequestBody UpdateBomRequestDTO dto) {
	    bomService.updateBom(dto);
	    return ResponseEntity.ok().build();
	}

	// ✅ 삭제 (여러 ID 삭제)
	@DeleteMapping
	public ResponseEntity<Void> deleteBoms(@RequestBody List<Long> parentIds) {
	    bomService.deleteBomsByParentIds(parentIds);
	    return ResponseEntity.ok().build();
	}

	// ✅ 목록 조회 (검색, 페이징)
	@GetMapping("/list")
	public ResponseEntity<BomListResponseDTO> getBomList(
	        @RequestParam(name = "searchField", required = false) String searchField,
	        @RequestParam(name = "keyword", required = false) String keyword,
	        @RequestParam(name = "useYn", required = false) String useYn,
	        @PageableDefault(size = 10) Pageable pageable) {

	    
	    List<BomProductItemDTO> productList = bomService.getParentProductDTOList();
	    ItemSelectionDTO itemSelectionDTO = bomService.getSelectableItems();

	    Page<BomProductItemDTO> bomPage = bomService.getPagedParentProductList(searchField, keyword, useYn, pageable);

	    BomListResponseDTO response = new BomListResponseDTO(
	        bomPage.getContent(),
	        bomPage.getTotalPages(),
	        bomPage.getNumber(),
	        bomService.getParentProductDTOList(),
	        itemSelectionDTO.getMaterials(),
	        itemSelectionDTO.getProducts()
	    );
	    System.out.println("흠");
	    return ResponseEntity.ok(response);
	}
	
	@DeleteMapping("/version/{versionId}")//삭제
	public ResponseEntity<?> deleteBomVersion(@PathVariable("versionId") Long versionId) {
	    bomService.deleteBomVersion(versionId);
	    return ResponseEntity.ok().build();
	}
	   // ✅ BOM 버전 상세 조회 (수정용)
    @GetMapping("/version/{versionId}")
    public ResponseEntity<BomEditResponseDTO> getBomVersionDetail(@PathVariable("versionId") Long versionId) {
        BomEditResponseDTO response = bomService.getBomEditData(versionId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/group/{parentItemId}")
    public ResponseEntity<Void> deleteBomGroup(@PathVariable("parentItemId") Long parentItemId) {
        bomService.deleteBomGroupByProductId(parentItemId);
        return ResponseEntity.noContent().build();
    }
    /**
     * ★ 추가 ★
     * versionId 기준 – 해당 버전에 속한 자식 노드만 포함한 트리
     */
    @GetMapping("/tree/version/{versionId}")
    public BomTreeDTO getBomTreeByVersion(@PathVariable("versionId") Long versionId) {
        return bomVersionService.getBomTreeByVersion(versionId);
    }
}
