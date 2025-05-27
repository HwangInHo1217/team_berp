package com.team.berp.bom.dto;

import java.util.List;

import com.team.berp.domain.Item;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BomListResponseDTO {
    private List<BomProductItemDTO> content;
    private int totalPages;
    private int currentPage;

    private List<BomProductItemDTO> productList;
    private List<Item> selectMaterialList;
    private List<Item> selectProductList;
}
