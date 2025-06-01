package com.team.berp.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BomEditResponseDTO {
    private Long versionId;
    private String versionCode;
    private String description;
    private String useYn;

    private Long parentItemId;
    private String parentCode;
    private String parentName;

    private List<BomListViewResponse.Component> components;
}
