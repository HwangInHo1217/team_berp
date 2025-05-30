package com.team.berp.bom.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.team.berp.bom.dto.*;
import com.team.berp.bom.service.BomService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bom")
public class BomApiController {

    private final BomService bomService;

    /** 1) 단일 버전 상세 조회 */
    @GetMapping("/version/{versionId}")
    public BomListViewResponse getBomByVersionId(@PathVariable Long versionId) {
        return bomService.getBomByVersionId(versionId);
    }

    /** 2) 부모 아이템별 버전 목록 조회 */
    @GetMapping("/versions/{parentId}")
    public List<BomVersionResponseDTO> getVersionsByParentId(@PathVariable Long parentId) {
        return bomService.getVersionsByParentId(parentId);
    }

    /** 3) 새 버전 등록 */
    @PostMapping
    public ResponseEntity<Void> registerBom(@RequestBody AddBomRequestDTO dto) {
        bomService.registerBomWithVersion(dto);
        return ResponseEntity.ok().build();
    }

    /** 4) 버전 + BOM 구성 수정 */
    @PutMapping
    public ResponseEntity<Void> updateBom(@RequestBody UpdateBomRequestDTO dto) {
        bomService.updateBom(dto);
        return ResponseEntity.ok().build();
    }

    /** 5) 버전별 BOM 일괄 삭제 */
    @DeleteMapping
    public ResponseEntity<Void> deleteBoms(@RequestBody List<Long> parentIds) {
        bomService.deleteBomsByParentIds(parentIds);
        return ResponseEntity.ok().build();
    }

    /** 6) 페이징 + 검색된 완제품 리스트 조회 */
    @GetMapping("/list")
    public ResponseEntity<BomListResponseDTO> getBomList(
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String useYn,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<BomProductItemDTO> page = bomService.getPagedParentProductList(searchField, keyword, useYn, pageable);
        ItemSelectionDTO        sel  = bomService.getSelectableItems();

        BomListResponseDTO resp = new BomListResponseDTO(
            page.getContent(),
            page.getTotalPages(),
            page.getNumber(),
            sel.getProducts(),
            sel.getMaterials()
        );
        return ResponseEntity.ok(resp);
    }
}
