package com.team.berp.mrp.service;

import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.bom.dto.BomListViewResponse; // (네가 올린 DTO 활용)
import org.springframework.data.domain.Page;
import java.util.List;

public interface MrpService {
    Page<MrpViewDto> findMrpList(
        int page, int size, String sortKey, String sortDir,
        String startDate, String endDate, String itemSearch, String custSearch
    );

    List<BomListViewResponse> findBomByItemCode(String itemCode);
}
