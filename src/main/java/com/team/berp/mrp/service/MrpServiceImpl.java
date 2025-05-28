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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MrpServiceImpl implements MrpService {
    private final EntityMrpRepository mrpRepository;
    private final EntityItemRepository itemRepository;
    private final EntityStockRepository stockRepository;
    private final BomRepository bomRepository;
    // 추가!
    private final EntityProdPlanRepository prodPlanRepository;
    //private final OrderLineItemRepository orderLineItemRepository;

    @Override
    public Page<MrpViewDto> findMrpList(
            int page, int size, String sortKey, String sortDir,
            String startDate, String endDate, String itemSearch, String custSearch
    ) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortKey));
        Page<Mrp> mrpPage = mrpRepository.findAll(pageable);

        List<Item> allItems = itemRepository.findAll();
        List<Stock> allStocks = stockRepository.findAll();

        List<MrpViewDto> dtoList = mrpPage.getContent().stream().map(mrp -> {
            Item item = mrp.getItem();
            int stockQty = 0;
            String custName = "-";
            String spec = "-";
            int prodQty = 0;
            int orderQty = 0;

            if (item != null) {
                // 1. 현재고
                stockQty = allStocks.stream()
                        .filter(s -> s.getItem() != null && s.getItem().getCode().equals(item.getCode()))
                        .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0)
                        .sum();
                // 2. 거래처명
                custName = mrpRepository.findLatestCompanyNameByItemId(item.getId());
                // 3. 규격
                spec = item.getSpec();
                // 4. 생산수량 (생산계획 테이블)
                Integer prodQtyObj = prodPlanRepository.findLatestProdQtyByItemId(item.getId());
                prodQty = prodQtyObj != null ? prodQtyObj : 0;
                // 5. 주문수량 (order_line_item 테이블)
               // Integer orderQtyObj = orderLineItemRepository.findLatestOrderQtyByItemId(item.getId());
                //orderQty = orderQtyObj != null ? orderQtyObj : 0;
            }

            return new MrpViewDto(
            	    item != null ? (item.getCode() != null ? item.getCode() : "") : "",     // itemCode
            	    item != null ? item.getName() : "",                                     // itemName
            	    item != null && item.getType() != null ? item.getType().toString() : "",// itemType
            	    item != null ? item.getUnit() : "",                                     // unit
            	    "",                                                                     // baseDate
            	    mrp.getRequiredQty() != null ? mrp.getRequiredQty() : 0,                // requiredQty
            	    stockQty,                                                               // stockQty
            	    0,                                                                      // confirmedQty
            	    mrp.getRequiredQty() != null ? mrp.getRequiredQty() - stockQty : 0,     // shortageQty
            	    "",                                                                     // source
            	    0,                                                                      // leadTime
            	    "",                                                                     // comment
            	    custName,                                                               // custName
            	    spec,                                                                   // spec
            	    prodQty,                                                                // prodQty
            	    orderQty                                                                // orderQty
            	);
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, mrpPage.getTotalElements());
    }

 // 2. BOM(투입자재) 리스트 (itemCode 기준)
    @Override
    public List<BomListViewResponse> findBomByItemCode(String itemCode) {
        if (itemCode == null || itemCode.isEmpty()) {
            return Collections.emptyList();
        }
        // 1) Item 찾기 (stream으로도 되고, findByCode 있으면 그걸로!)
        List<Item> allItems = itemRepository.findAll();
        Item item = allItems.stream()
                .filter(i -> i.getCode() != null && i.getCode().equals(itemCode))
                .findFirst()
                .orElse(null);
        if (item == null) return Collections.emptyList();

        // 2) BOM 테이블에서 parentItem으로 BOM 리스트 조회
        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(item);

        // 3) BOM → Component 리스트로 변환
        List<BomListViewResponse.Component> components = bomList.stream()
            .map(bom -> new BomListViewResponse.Component(
                bom.getChildItem().getCode(),
                bom.getChildItem().getName(),
                bom.getQty() // 실제 수량 필드명에 맞춰서! (예: getQuantity → getQty)
            ))
            .collect(Collectors.toList());

        // 4) 최종 응답 DTO(부모 코드/이름 + 부품 리스트)로 감싸기
        BomListViewResponse response = new BomListViewResponse(
            item.getCode(),
            item.getName(),
            components
        );
        // 5) List로 반환 (프론트/타 팀원 호환 위해)
        return Collections.singletonList(response);
    }
}
