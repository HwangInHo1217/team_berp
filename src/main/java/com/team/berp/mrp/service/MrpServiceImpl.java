package com.team.berp.mrp.service;

import com.team.berp.domain.Item;
import com.team.berp.domain.Mrp;
import com.team.berp.domain.Stock;
import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.bom.dto.BomListViewResponse;
import com.team.berp.mrp.repository.EntityMrpRepository;
import com.team.berp.mrp.repository.EntityProdPlanRepository;
import com.team.berp.mrp.repository.EntityItemRepository;
import com.team.berp.mrp.repository.EntityStockRepository;
import com.team.berp.bom.repository.BomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MrpServiceImpl implements MrpService {
    private final EntityMrpRepository mrpRepository;
    private final EntityItemRepository itemRepository;
    private final EntityStockRepository stockRepository;
    private final BomRepository bomRepository;
    private final EntityProdPlanRepository prodPlanRepository;

    @Override
    public Page<MrpViewDto> findMrpList(
        int page, int size, String sortKey, String sortDir,
        String startDate, String endDate, String itemSearch, String custSearch
    ) {
        Sort.Direction dir = sortDir.equalsIgnoreCase("asc")
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(dir, sortKey));
        Page<Mrp> mrpPage = mrpRepository.findAll(pageable);

        // 재고 캐시
        List<Stock> allStocks = stockRepository.findAll();

        List<MrpViewDto> dtoList = mrpPage.getContent().stream().map(mrp -> {
            Item item = mrp.getItem();
            int stockQty = allStocks.stream()
                .filter(s -> s.getItem() != null && s.getItem().getCode().equals(item.getCode()))
                .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                .sum();

            String custName = mrpRepository.findLatestCompanyNameByItemId(item.getId());
            String spec     = item.getSpec();
            int prodQty     = Optional.ofNullable(
                prodPlanRepository.findLatestProdQtyByItemId(item.getId())
            ).orElse(0);

            int required = Optional.ofNullable(mrp.getRequiredQty()).orElse(0);
            int shortage = Math.max(required - stockQty, 0);

            // NEW ▶ 납기 요청일, MRP 상태
            String dueDate   = mrp.getDueDate() != null ? mrp.getDueDate().toString() : "";
            String mrpStatus = mrp.getStatus() != null  ? mrp.getStatus().toString() : "";

            return new MrpViewDto(
                item.getCode(),
                item.getName(),
                item.getType().toString(),
                item.getUnit(),
                Optional.ofNullable(mrp.getBaseDate()).map(Object::toString).orElse(""),
                required,
                stockQty,
                0,             // 확정수량
                shortage,
                mrp.getSource(),
                Optional.ofNullable(mrp.getLeadTime()).orElse(0),
                mrp.getComment(),
                custName,
                spec,
                prodQty,
                0,             // orderQty (필요 시 구현)
                dueDate,
                mrpStatus
            );
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, mrpPage.getTotalElements());
    }

    @Override
    public List<BomListViewResponse> findBomByItemCode(String itemCode) {
        if (itemCode == null || itemCode.isEmpty()) {
            return Collections.emptyList();
        }
        Item parent = itemRepository.findAll().stream()
            .filter(i -> itemCode.equals(i.getCode()))
            .findFirst().orElse(null);
        if (parent == null) return Collections.emptyList();

        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(parent);
        List<Stock> allStocks = stockRepository.findAll();
        LocalDate today = LocalDate.now();

        List<BomListViewResponse.Component> components = bomList.stream().map(bom -> {
            Item child = bom.getChildItem();
            int requiredQty      = bom.getQty();
            int stockQty         = allStocks.stream()
                .filter(s -> s.getItem() != null && s.getItem().getCode().equals(child.getCode()))
                .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                .sum();
            int shortageQty      = Math.max(requiredQty - stockQty, 0);
            int safetyStock      = Optional.ofNullable(child.getSafetyStock()).orElse(0);
            int purchaseQty      = shortageQty + safetyStock;
            int purchaseLeadTime = Optional.ofNullable(child.getPurchaseLeadTime()).orElse(0);
            String expectedDate  = today.plusDays(purchaseLeadTime).toString();

            return new BomListViewResponse.Component(
            	    child.getCode(),                                        // 자재 코드
            	    child.getName(),                                        // 자재명
            	    requiredQty,                                            // 소요량
            	    child.getSpec(),                                        // 규격
            	    child.getUnit(),                                        // 단위
            	    bom.getSeqNo(),                                         // 순번
            	    Optional.ofNullable(bom.getLossRt()).map(Object::toString).orElse("0%"), // 손실률
            	    Optional.ofNullable(bom.getItemPrice()).map(String::valueOf).orElse("0"), // 단가
            	    bom.getRemark()                                         // 비고
            	);

        }).collect(Collectors.toList());

        return Collections.singletonList(
            new BomListViewResponse(parent.getCode(), parent.getName(), components)
        );
    }
}
