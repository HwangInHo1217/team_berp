package com.team.berp.bom.dto;

import java.util.List;

import com.team.berp.domain.Item;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemSelectionDTO {
    private List<Item> products;
    private List<Item> materials;
}
