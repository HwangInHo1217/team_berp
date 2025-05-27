package com.team.berp.bom.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.team.berp.domain.Item;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BomListResponseDTO {
    private Page<BomProductItemDTO> bomPage;           // 페이징된 BOM 리스트
    private List<BomProductItemDTO> productList;        // 완제품 리스트 (등록 시 사용)
    private List<Item> selectMaterialList;              // 선택 가능한 자재 리스트
    private List<Item> selectProductList;               // 선택 가능한 제품 리스트
}
