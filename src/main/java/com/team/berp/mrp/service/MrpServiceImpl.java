// File: /Team_BERP/src/main/java/com/team/berp/mrp/service/MrpServiceImpl.java
package com.team.berp.mrp.service;

import com.team.berp.domain.Item;
import com.team.berp.domain.Mrp;
import com.team.berp.domain.Stock;
import com.team.berp.mrp.dto.MrpDetailDto;
import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.mrp.dto.ExtendedBomListViewResponse;
import com.team.berp.mrp.dto.ExtendedBomListViewResponse.ExtendedComponent;
import com.team.berp.mrp.repository.EntityMrpRepository;
import com.team.berp.mrp.repository.EntityProdPlanRepository;
import com.team.berp.mrp.repository.EntityItemRepository;
import com.team.berp.mrp.repository.EntityStockRepository;
import com.team.berp.bom.repository.BomRepository;
import com.team.berp.bom.dto.BomListViewResponse;
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

            String dueDate   = mrp.getDueDate() != null ? mrp.getDueDate().toString() : "";
            String mrpStatus = mrp.getStatus() != null  ? mrp.getStatus().toString() : "";

            return new MrpViewDto(
                mrp.getMrpId(),
                item.getCode(),
                item.getName(),
                item.getType().toString(),
                item.getUnit(),
                Optional.ofNullable(mrp.getBaseDate()).map(Object::toString).orElse(""),
                required,
                stockQty,
                0,
                shortage,
                mrp.getSource(),
                Optional.ofNullable(mrp.getLeadTime()).orElse(0),
                mrp.getComment(),
                custName,
                spec,
                prodQty,
                0,
                dueDate,
                mrpStatus
            );
        }).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, mrpPage.getTotalElements());
    }

    @Override
    public List<ExtendedBomListViewResponse> findBomByItemCode(String itemCode) {
        if (itemCode == null || itemCode.isEmpty()) {
            return Collections.emptyList();
        }

        Item parent = itemRepository.findAll().stream()
            .filter(i -> itemCode.equals(i.getCode()))
            .findFirst().orElse(null);
        if (parent == null) {
            return Collections.emptyList();
        }

        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(parent);
        List<Stock> allStocks = stockRepository.findAll();
        LocalDate today = LocalDate.now();

        List<ExtendedComponent> extComps = bomList.stream().map(bom -> {
            Item child = bom.getChildItem();
            int requiredQty       = bom.getQty();
            int stockQty          = allStocks.stream()
                .filter(s -> s.getItem() != null &&
                             s.getItem().getCode().equals(child.getCode()))
                .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                .sum();

            int shortageQty       = Math.max(requiredQty - stockQty, 0);
            int safetyStock       = Optional.ofNullable(child.getSafetyStock()).orElse(0);
            int remainingAfterUse = stockQty - requiredQty;
            int purchaseQty       = (remainingAfterUse >= safetyStock)
                                     ? 0
                                     : (safetyStock - remainingAfterUse);
            int purchaseLeadTime  = Optional.ofNullable(child.getPurchaseLeadTime()).orElse(0);
            String expectedDate   = today.plusDays(purchaseLeadTime).toString();

            // build base Component
            BomListViewResponse.Component base = new BomListViewResponse.Component(
                child.getCode(),
                child.getName(),
                requiredQty,
                child.getSpec(),
                child.getUnit(),
                bom.getSeqNo(),
                Optional.ofNullable(bom.getLossRt()).map(Object::toString).orElse("0%"),
                Optional.ofNullable(bom.getItemPrice()).map(String::valueOf).orElse("0"),
                bom.getRemark()
            );

            return new ExtendedComponent(
                base,
                stockQty,
                shortageQty,
                safetyStock,
                purchaseQty,
                purchaseLeadTime,
                expectedDate
            );
        }).collect(Collectors.toList());

        ExtendedBomListViewResponse wrapper = new ExtendedBomListViewResponse(
            parent.getCode(),
            parent.getName(),
            extComps
        );

        return Collections.singletonList(wrapper);
    }

    @Override
    public MrpDetailDto findMrpDetailById(Long mrpId) {
        Mrp mrp = mrpRepository.findById(mrpId).orElse(null);
        if (mrp == null) {
            return null;
        }
        Item item = mrp.getItem();
        LocalDate today = LocalDate.now();

        Long id            = mrp.getMrpId();
        String createDate  = mrp.getBaseDate() != null ? mrp.getBaseDate().toString() : "";
        String dueDate     = mrp.getDueDate()  != null ? mrp.getDueDate().toString() : "";
        String planType    = mrp.getSource()   != null ? mrp.getSource() : "";
        String status      = mrp.getStatus()   != null ? mrp.getStatus().toString() : "";
        String ownerName   = ""; // 담당자/부서

        String itemCode    = item.getCode();
        String itemName    = item.getName();
        String itemType    = item.getType().toString();
        String unit        = item.getUnit();
        String spec        = item.getSpec();
        int safetyStock    = Optional.ofNullable(item.getSafetyStock()).orElse(0);

        List<Stock> allStocks = stockRepository.findAll();
        int stockQty = allStocks.stream()
            .filter(s -> s.getItem() != null && s.getItem().getCode().equals(itemCode))
            .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
            .sum();
        String location = ""; // 재고 위치

        int requiredQty       = Optional.ofNullable(mrp.getRequiredQty()).orElse(0);
        int shortageQty       = Math.max(requiredQty - stockQty, 0);
        int purchaseLeadTime  = Optional.ofNullable(item.getPurchaseLeadTime()).orElse(0);
        int productionLeadTime = Optional.ofNullable(mrp.getLeadTime()).orElse(0);
        String orderableDate  = today.plusDays(Math.min(purchaseLeadTime, productionLeadTime)).toString();

        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(item);
        List<MrpDetailDto.MrpBomComponent> bomComponents = bomList.stream().map(bom -> {
            Item child = bom.getChildItem();
            int perParentQty      = bom.getQty();
            int totalQty          = perParentQty * requiredQty;
            int childStockQty     = allStocks.stream()
                .filter(s -> s.getItem() != null && s.getItem().getCode().equals(child.getCode()))
                .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                .sum();
            int childShortageQty  = Math.max(totalQty - childStockQty, 0);
            int childLeadTime     = Optional.ofNullable(child.getPurchaseLeadTime()).orElse(0);
            String supplier       = ""; // 공급처

            return new MrpDetailDto.MrpBomComponent(
                child.getCode(),
                child.getName(),
                perParentQty,
                totalQty,
                childStockQty,
                childShortageQty,
                childLeadTime,
                supplier
            );
        }).collect(Collectors.toList());

        List<MrpDetailDto.MrpPurchaseOrder> purchaseOrders = new ArrayList<>();
        List<MrpDetailDto.MrpWorkOrder> workOrders         = new ArrayList<>();
        List<MrpDetailDto.MrpHistory> history               = new ArrayList<>();

        return new MrpDetailDto(
            id, createDate, dueDate, planType, status, ownerName,
            itemCode, itemName, itemType, unit, spec, safetyStock, stockQty, location,
            requiredQty, shortageQty, purchaseLeadTime, productionLeadTime, orderableDate,
            bomComponents, purchaseOrders, workOrders, history
        );
    }
}
