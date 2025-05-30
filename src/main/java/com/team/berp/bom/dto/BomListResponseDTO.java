package com.team.berp.bom.dto;

import java.util.List;

import com.team.berp.domain.Item;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BomListResponseDTO {
    private final List<BomProductItemDTO> content;
    private final int totalPages;
    private final int pageNumber;
    private final List<Item> products;
    private final List<Item> materials;
}

