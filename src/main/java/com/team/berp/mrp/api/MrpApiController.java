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

    // 상단 리스트
    @GetMapping("/list")
    public Map<String, Object> getMrpList(
            @RequestParam(name="page", defaultValue = "1") int page,
            @RequestParam(name="size", defaultValue = "5") int size,
            @RequestParam(name="sortKey", defaultValue = "mrpId") String sortKey,
            @RequestParam(name="sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name="startDate", required = false) String startDate,
            @RequestParam(name="endDate", required = false) String endDate,
            @RequestParam(name="itemSearch", required = false) String itemSearch,
            @RequestParam(name="custSearch", required = false) String custSearch
    ) {
        Page<MrpViewDto> result = mrpService.findMrpList(
            page, size, sortKey, sortDir, startDate, endDate, itemSearch, custSearch
        );
        Map<String, Object> response = new HashMap<>();
        response.put("content", result.getContent());
        response.put("currentPage", result.getNumber() + 1);
        response.put("totalPages", result.getTotalPages());
        response.put("totalElements", result.getTotalElements());
        return response;
    }

    // 하단 리스트 (BOM)
    @GetMapping("/bom/{itemCode}")
    public List<BomListViewResponse> getBomList(@PathVariable String itemCode) {
        return mrpService.findBomByItemCode(itemCode);
    }
}
