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
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortKey));
        Page<Mrp> mrpPage = mrpRepository.findAll(pageable);

        List<Stock> allStocks = stockRepository.findAll();

        List<MrpViewDto> dtoList = mrpPage.getContent().stream().map(mrp -> {
            Item item = mrp.getItem();
            int stockQty = allStocks.stream()
                .filter(s -> s.getItem() != null && s.getItem().getCode().equals(item.getCode()))
                .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0)
                .sum();

            String custName = mrpRepository.findLatestCompanyNameByItemId(item.getId());
            String spec = item.getSpec();

            Integer prodQtyObj = prodPlanRepository.findLatestProdQtyByItemId(item.getId());
            int prodQty = prodQtyObj != null ? prodQtyObj : 0;

            int orderQty = 0; // 필요 시 추가 구현

            int required = mrp.getRequiredQty() != null ? mrp.getRequiredQty() : 0;
            int shortage = Math.max(required - stockQty, 0);

            String dueDate = mrp.getDueDate() != null ? mrp.getDueDate().toString() : "";
            String mrpStatus = mrp.getStatus() != null ? mrp.getStatus().toString() : "";

            return new MrpViewDto(
                item.getCode(),                  
                item.getName(),                  
                item.getType().toString(),       
                item.getUnit(),                  
                mrp.getBaseDate() != null ? mrp.getBaseDate().toString() : "",      
                required,                        
                stockQty,                        
                0,                               
                shortage,                        
                mrp.getSource(),                 
                mrp.getLeadTime() != null ? mrp.getLeadTime() : 0,               
                mrp.getComment(),                
                custName,                        
                spec,                            
                prodQty,                         
                orderQty,                        
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

        Optional<Item> parentOpt = itemRepository.findAll().stream()
            .filter(i -> itemCode.equals(i.getCode()))
            .findFirst();
        if (parentOpt.isEmpty()) return Collections.emptyList();
        Item parent = parentOpt.get();

        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(parent);

        List<Stock> allStocks = stockRepository.findAll();
        LocalDate today = LocalDate.now();

        List<BomListViewResponse.Component> components = bomList.stream().map(bom -> {
            Item child = bom.getChildItem();
            int requiredQty = bom.getQty();
            int stockQty = allStocks.stream()
                .filter(s -> s.getItem() != null && s.getItem().getCode().equals(child.getCode()))
                .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0)
                .sum();
            int shortageQty = Math.max(requiredQty - stockQty, 0);
            int safetyStock = child.getSafetyStock() != null ? child.getSafetyStock() : 0;
            int purchaseQty = shortageQty + safetyStock;
            int purchaseLeadTime = child.getPurchaseLeadTime() != null ? child.getPurchaseLeadTime() : 0;
            String expectedDate = today.plusDays(purchaseLeadTime).toString();

            return new BomListViewResponse.Component(
                child.getCode(),           // childCode
                child.getName(),           // childName
                requiredQty,               // qty
                child.getSpec(),           // spec
                child.getUnit(),           // unit
                bom.getSeqNo(),                                        // seqNo
                bom.getLossRt() != null ? bom.getLossRt().toString() : null, // lossRate
                bom.getItemPrice() != null ? bom.getItemPrice().toString() : null, // unitPrice
                bom.getRemark(),           // remark
                stockQty,                  // NEW: 현재고
                shortageQty,               // NEW: 부족수량
                safetyStock,               // NEW: 안전재고
                purchaseQty,               // NEW: 발주필요수량
                purchaseLeadTime,          // NEW: 구매 리드타임
                expectedDate               // NEW: 예상입고일
            );
        }).collect(Collectors.toList());

        // 부모코드/이름 + components 리스트 반환 (List<BomListViewResponse> 구조 유지)
        BomListViewResponse response = new BomListViewResponse(
            parent.getCode(),    // parentCode
            parent.getName(),    // parentName
            components
        );
        return Collections.singletonList(response);
    }
}
