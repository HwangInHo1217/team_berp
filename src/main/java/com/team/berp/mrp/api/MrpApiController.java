package com.team.berp.mrp.api;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import com.team.berp.domain.Item;
import com.team.berp.domain.Mrp;
import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.mrp.repository.EntityMrpRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MrpApiController {

    @Autowired
    private final EntityMrpRepository repo;

    // 정렬키/방향 받아서 itemName만 JPQL로 분기, 나머지는 기본 JPA 정렬
    @GetMapping("/api/mrp/list")
    public Map<String, Object> getMrpList(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "5") int size,
        @RequestParam(name = "sortKey", defaultValue = "mrpId") String sortKey,
        @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir
    ) {
        Page<Mrp> result;
        Pageable pageable = PageRequest.of(page - 1, size);

        // itemName 정렬만 직접 쿼리, 나머지는 기본 제공
        if ("itemName".equals(sortKey)) {
            if ("asc".equalsIgnoreCase(sortDir)) {
                result = repo.findAllOrderByItemNameAsc(pageable);
            } else {
                result = repo.findAllOrderByItemNameDesc(pageable);
            }
        } else {
            // JPA 기본 정렬
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable sortPageable = PageRequest.of(page - 1, size, Sort.by(direction, sortKey));
            result = repo.findAll(sortPageable);
        }

        // DTO 변환
        List<MrpViewDto> dtoList = new ArrayList<>();
        for (Mrp mrp : result.getContent()) {
            Item item = mrp.getItem();
            dtoList.add(new MrpViewDto(
                item != null ? String.valueOf(item.getItemId()) : "-",
                item != null ? item.getItemName() : "-",
                item != null ? (item.getItemType() != null ? item.getItemType().toString() : "-") : "-",
                item != null ? item.getUnit() : "-",
                "-", // baseDate (필요시 plan에서 추출)
                mrp.getRequiredQty() != null ? mrp.getRequiredQty() : 0,
                0, // stockQty (추가 필요)
                0, // confirmedQty
                0, // shortageQty
                "-", // source
                0, // leadTime
                "-" // comment
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", dtoList);
        response.put("totalPages", result.getTotalPages());
        response.put("totalElements", result.getTotalElements());
        response.put("currentPage", result.getNumber() + 1);
        return response;
    }
}
