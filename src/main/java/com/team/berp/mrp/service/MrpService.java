// File: /Team_BERP/src/main/java/com/team/berp/mrp/service/MrpService.java
package com.team.berp.mrp.service;

import com.team.berp.mrp.dto.MrpDetailDto;
import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.mrp.dto.ExtendedBomListViewResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MrpService {
    /**
     * 상단 MRP 목록 조회
     */
    Page<MrpViewDto> findMrpList(
        int page,
        int size,
        String sortKey,
        String sortDir,
        String startDate,
        String endDate,
        String itemSearch  
    );

    /**
     * 하단 BOM(투입자재) 리스트 조회
     * → 반환 타입을 List<ExtendedBomListViewResponse>로 맞춥니다.
     */
    List<ExtendedBomListViewResponse> findBomByItemCode(String itemCode);

    /**
     * 특정 MRP 상세 정보 조회
     */
    MrpDetailDto findMrpDetailById(Long mrpId);
}
