package com.team.berp.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BomVersionResponseDTO {
    private Long id;           // bom_version_id
    private String versionCode;
    private String useYn;
}
