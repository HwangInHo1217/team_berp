// File: src/main/java/com/team/berp/mrp/service/MrpService.java
package com.team.berp.mrp.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.team.berp.mrp.dto.ExtendedBomListViewResponse;
import com.team.berp.mrp.dto.MrpDetailDto;
import com.team.berp.mrp.dto.MrpViewDto;

public interface MrpService {

    Page<MrpViewDto> findMrpList(
        int page, int size,
        String sortKey, String sortDir,
        String startDate, String endDate,
        String itemSearch
    );

    List<ExtendedBomListViewResponse> findBomByItemCode(String itemCode);

    MrpDetailDto findMrpDetailById(Long mrpId);

    void generateMrpForOrder(Long orderId);

    // 신규 메서드: 주문 상세(ID)와 부족 수량을 받아 MRP 생성
    void generateMrpForOrderLineItem(Long orderLineItemId, int neededQty);
}
