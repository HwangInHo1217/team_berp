package com.team.berp.mrp.api;

import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.bom.dto.BomListViewResponse;
import com.team.berp.mrp.service.MrpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/mrp")
@RequiredArgsConstructor
public class MrpApiController {
    private final MrpService mrpService;

    /**
     * [상단 리스트] MRP 목록 조회 API
     * 반환되는 JSON content 배열에는
     *  • stockQty, shortageQty            (현재고/부족수량)
     *  • dueDate                           (납기요청일)        ← NEW
     *  • leadTime                          (리드타임)
     *  • mrpStatus                         (MRP 상태)        ← NEW
     *  • prodQty, orderQty, custName, spec
     */
    @GetMapping("/list")
    public Map<String,Object> getMrpList(
            @RequestParam(name="page",     defaultValue="1")            int page,
            @RequestParam(name="size",     defaultValue="5")            int size,
            @RequestParam(name="sortKey",  defaultValue="mrpId")        String sortKey,
            @RequestParam(name="sortDir",  defaultValue="desc")         String sortDir,
            @RequestParam(name="startDate", required=false)             String startDate,
            @RequestParam(name="endDate",   required=false)             String endDate,
            @RequestParam(name="itemSearch",required=false)             String itemSearch,
            @RequestParam(name="custSearch",required=false)             String custSearch
    ) {
        Page<MrpViewDto> result = mrpService.findMrpList(
            page, size, sortKey, sortDir, startDate, endDate, itemSearch, custSearch
        );
        Map<String,Object> response = new HashMap<>();
        response.put("content",       result.getContent());
        response.put("currentPage",   result.getNumber() + 1);
        response.put("totalPages",    result.getTotalPages());
        response.put("totalElements", result.getTotalElements());
        return response;
    }

    /**
     * [하단 리스트] 선택한 품목의 BOM(투입자재) 리스트 조회 API
     * 반환 타입 BomListViewResponse에도 아래 필드가 포함되어야 합니다:
     *  • subItemCode, subItemName, spec, unit, qty       (기존)
     *  • stockQty                                        (현재고)          ← NEW
     *  • shortageQty                                     (부족수량)        ← NEW
     *  • safetyStock                                     (안전재고)        ← NEW
     *  • purchaseQty                                     (발주필요수량)    ← NEW
     *  • purchaseLeadTime                                (리드타임(구매)) ← NEW
     *  • expectedDate                                    (예상입고일)      ← NEW
     */
    @GetMapping("/bom/{itemCode}")
    public List<BomListViewResponse> getBomList(@PathVariable String itemCode) {
        return mrpService.findBomByItemCode(itemCode);
    }
}
