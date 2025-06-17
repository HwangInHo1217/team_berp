// File: src/main/java/com/team/berp/mrp/service/MrpServiceImpl.java
package com.team.berp.mrp.service;

import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.Item;
import com.team.berp.domain.Mrp;
import com.team.berp.domain.MrpStatus;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.domain.ProdPlan;
import com.team.berp.domain.ProdPlan.PlanStatus;
import com.team.berp.domain.ProdOrder;
import com.team.berp.domain.Stock;
import com.team.berp.domain.InventoryLog;
import com.team.berp.domain.Warehouse;

import com.team.berp.mrp.dto.MrpDetailDto;
import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.mrp.dto.ExtendedBomListViewResponse;
import com.team.berp.mrp.dto.ExtendedBomListViewResponse.ExtendedComponent;
import com.team.berp.bom.dto.BomListViewResponse;

import com.team.berp.bom.repository.BomRepository;
import com.team.berp.mrp.repository.EntityMrpRepository;
import com.team.berp.mrp.repository.EntityItemRepository;
import com.team.berp.mrp.repository.EntityStockRepository;
import com.team.berp.mrp.repository.EntityProdPlanRepository;
import com.team.berp.mrp.repository.Mrp_OrderLineItemRepository;
import com.team.berp.mrp.repository.Mrp_CompanyOrderRepository;
import com.team.berp.mrp.repository.Mrp_ProdOrderRepository;
import com.team.berp.mrp.repository.Mrp_InventoryLogRepository;
import com.team.berp.mrp.repository.Mrp_WarehouseRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MrpServiceImpl implements MrpService {

    private final EntityMrpRepository       mrpRepository;
    private final EntityItemRepository      itemRepository;
    private final EntityStockRepository     stockRepository;
    private final BomRepository             bomRepository;
    private final EntityProdPlanRepository  prodPlanRepository;

    private final Mrp_OrderLineItemRepository  mrpOrderLineItemRepository;
    private final Mrp_CompanyOrderRepository   mrpCompanyOrderRepository;
    private final Mrp_ProdOrderRepository      mrpProdOrderRepository;
    private final Mrp_InventoryLogRepository   mrpInventoryLogRepository;
    private final Mrp_WarehouseRepository      mrpWarehouseRepository;

    @Override
    public Page<MrpViewDto> findMrpList(
            int page, int size,
            String sortKey, String sortDir,
            String startDate, String endDate,
            String itemSearch
    ) {
        Sort.Direction dir = sortDir.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(dir, sortKey));

        LocalDate start = (startDate == null || startDate.isEmpty())
                ? LocalDate.MIN
                : LocalDate.parse(startDate);
        LocalDate end   = (endDate   == null || endDate.isEmpty())
                ? LocalDate.MAX
                : LocalDate.parse(endDate);

        Page<Mrp> mrpPage;
        if (itemSearch == null || itemSearch.isEmpty()) {
        	     // 검색어 없으면 PLANNED 전체(날짜 범위 기준) 조회
        	     mrpPage = mrpRepository
        	         .findByStatusAndBaseDateBetween(
        	             MrpStatus.PLANNED,
        	             start, end,
        	             pageable
        	         );
        	 } else {
        	     // 검색어 있으면 PLANNED + 날짜 + 코드/이름 검색
        	     mrpPage = mrpRepository
        	         .findByStatusAndBaseDateBetweenAndPlan_Item_CodeContainingIgnoreCaseOrBaseDateBetweenAndPlan_Item_NameContainingIgnoreCase(
        	             MrpStatus.PLANNED,
        	             start, end, itemSearch,
        	             start, end, itemSearch,
        	             pageable
        	         );
        	 }

        List<Stock> allStocks = stockRepository.findAll();
        Map<String, Integer> stockByItemCode = allStocks.stream()
                .filter(s -> s.getItem() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getItem().getCode(),
                        Collectors.summingInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                ));

        Map<String, Integer> totalRequiredByItemCode = new HashMap<>();
        for (Mrp mrp : mrpPage.getContent()) {
            Item parent = mrp.getPlan().getItem();
            if (parent == null) continue;

            Long itemId = parent.getId();
            String code = parent.getCode();
            Integer sumRequired = mrpRepository.sumRequiredQtyByItemIdAndStatus(
                    itemId, MrpStatus.PLANNED);
            int requiredQty = (sumRequired != null ? sumRequired : 0);

            totalRequiredByItemCode.put(code, requiredQty);
        }

        Set<String> seenCodes = new HashSet<>();
        List<MrpViewDto> dtoList = new ArrayList<>();

        for (Mrp mrp : mrpPage.getContent()) {
            Item parent = mrp.getPlan().getItem();
            if (parent == null) continue;

            String itemCode = parent.getCode();
            if (seenCodes.contains(itemCode)) {
                continue;
            }
            seenCodes.add(itemCode);

            int stockQty = stockByItemCode.getOrDefault(itemCode, 0);
            int requiredQty = totalRequiredByItemCode.getOrDefault(itemCode, 0);
            int shortageQty = requiredQty - stockQty;
            if (shortageQty <= 0) {
                continue;
            }

            String itemName   = parent.getName();
            String itemType   = (parent.getType() != null ? parent.getType().toString() : "");
            String unit       = parent.getUnit();
            String spec       = parent.getSpec();
            String custName   = mrpRepository.findLatestCompanyNameByItemId(parent.getId());
            String dueDateStr = (mrp.getDueDate() != null ? mrp.getDueDate().toString() : "");
            String mrpStatus  = (mrp.getStatus() != null ? mrp.getStatus().toString() : "");

            MrpViewDto dto = new MrpViewDto(
                    mrp.getMrpId(),
                    itemCode,
                    itemName,
                    itemType,
                    unit,
                    Optional.ofNullable(mrp.getBaseDate()).map(Object::toString).orElse(""),
                    requiredQty,
                    stockQty,
                    shortageQty,
                    mrp.getSource(),
                    Optional.ofNullable(mrp.getLeadTime()).orElse(0),
                    mrp.getComment(),
                    custName,
                    spec,
                    dueDateStr,
                    mrpStatus
            );
            dtoList.add(dto);
        }

        return new PageImpl<>(dtoList, pageable, dtoList.size());
    }

    @Override
    public List<ExtendedBomListViewResponse> findBomByItemCode(String itemCode) {
        if (itemCode == null || itemCode.isEmpty()) {
            return Collections.emptyList();
        }
        Item parentItem = itemRepository.findByCode(itemCode);
        if (parentItem == null) {
            return Collections.emptyList();
        }
        
        // [수정 1] 부모의 '총 필요수량'을 가져옵니다. (모달 로직과 동일하게)
        Integer sumRequired = mrpRepository.sumRequiredQtyByItemIdAndStatus(parentItem.getId(), MrpStatus.PLANNED);
        int parentRequiredQty = (sumRequired != null) ? sumRequired : 0;
        
        // 재고 정보를 미리 Map으로 만들어 성능을 최적화합니다.
        Map<Long, Integer> stockMap = stockRepository.findAll().stream()
                .filter(s -> s.getItem() != null && s.getQuantity() != null)
                .collect(Collectors.groupingBy(s -> s.getItem().getId(), 
                                               Collectors.summingInt(Stock::getQuantity)));
        
        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(parentItem);
        LocalDate today = LocalDate.now();

        List<ExtendedComponent> extendedComponents = bomList.stream()
            .map(bom -> {
                Item childItem = bom.getChildItem();
                if (childItem == null) return null;
                
                int perParentQty = bom.getQty();

                // [수정 2] 자재 총 필요량 = (부모의 총 필요수량) * (BOM 단위 소요량)
                int totalQty = perParentQty * parentRequiredQty;

                int childStockQty = stockMap.getOrDefault(childItem.getId(), 0);
                int childShortageQty = Math.max(0, totalQty - childStockQty);
                int safetyStock = Optional.ofNullable(childItem.getSafetyStock()).orElse(0);
                int purchaseQty = (childShortageQty > 0) ? (childShortageQty + safetyStock) : 0;
                int purchaseLeadTime = Optional.ofNullable(childItem.getPurchaseLeadTime()).orElse(0);
                String expectedDate = today.plusDays(purchaseLeadTime).toString();

                return new ExtendedComponent(
                    new BomListViewResponse.Component(
                        childItem.getCode(),
                        childItem.getName(),
                        perParentQty,
                        childItem.getSpec(),
                        childItem.getUnit(),
                        bom.getSeqNo(),
                        Optional.ofNullable(bom.getLossRt()).map(Object::toString).orElse("0%"),
                        Optional.ofNullable(bom.getItemPrice()).map(String::valueOf).orElse("0"),
                        bom.getRemark()
                    ),
                    totalQty,
                    childStockQty,
                    childShortageQty,
                    safetyStock,
                    purchaseQty,
                    purchaseLeadTime,
                    expectedDate
                );
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        ExtendedBomListViewResponse wrapper = new ExtendedBomListViewResponse(
            parentItem.getCode(),
            parentItem.getName(),
            extendedComponents
        );
        return Collections.singletonList(wrapper);
    }

    @Override
    public MrpDetailDto findMrpDetailById(Long mrpId) {
        Mrp mrp = mrpRepository.findById(mrpId).orElse(null);
        if (mrp == null) {
            return null;
        }
        ProdPlan plan = mrp.getPlan();
        Item parentItem = (plan != null ? plan.getItem() : null);
        if (parentItem == null) {
            return null; 
        }

        Long     id           = mrp.getMrpId();
        String   createDate   = (mrp.getBaseDate() != null) ? mrp.getBaseDate().toString() : "";
        String   dueDate      = (mrp.getDueDate()  != null) ? mrp.getDueDate().toString()  : "";
        String   planType     = (mrp.getSource()   != null) ? mrp.getSource()   : "";
        String   status       = (mrp.getStatus() != null)   ? mrp.getStatus().toString()   : "";
        String   itemCode    = parentItem.getCode();
        String   itemName    = parentItem.getName();
        String   itemType    = (parentItem.getType() != null) ? parentItem.getType().toString() : "";
        String   unit        = parentItem.getUnit();
        String   spec        = parentItem.getSpec();
        int    safetyStock = Optional.ofNullable(parentItem.getSafetyStock()).orElse(0);

        List<Stock> allStocks = stockRepository.findAll();
        Map<String, Integer> stockByItemCode = allStocks.stream()
                .filter(s -> s.getItem() != null)
                .collect(Collectors.groupingBy(s -> s.getItem().getCode(),
                                               Collectors.summingInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))));
        
        int parentStockQty = stockByItemCode.getOrDefault(itemCode, 0);

        Integer sumRequired = mrpRepository.sumRequiredQtyByItemIdAndStatus(parentItem.getId(), MrpStatus.PLANNED);
        final int requestQty = (sumRequired != null) ? sumRequired : 0;
        int shortageQty = Math.max(0, requestQty - parentStockQty);

        int purchaseLeadTime = Optional.ofNullable(parentItem.getPurchaseLeadTime()).orElse(0);
        int productionLeadTime = Optional.ofNullable(mrp.getLeadTime()).orElse(0);
        long daysToOrderable = Math.min(purchaseLeadTime, productionLeadTime);
        String orderableDate = LocalDate.now().plusDays(daysToOrderable).toString();

        Map<String, Integer> warehouseMap = new LinkedHashMap<>();
        for (Stock s : allStocks) {
            if (s.getItem() != null && itemCode.equals(s.getItem().getCode())) {
                Warehouse wh = s.getWarehouse();
                if (wh != null) {
                    warehouseMap.put(wh.getWarehouseName(), warehouseMap.getOrDefault(wh.getWarehouseName(), 0) + s.getQuantity());
                }
            }
        }
        String location = warehouseMap.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(e -> String.format("%s(%d)", e.getKey(), e.getValue()))
            .collect(Collectors.joining("  "));

        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(parentItem);
        List<MrpDetailDto.MrpBomComponent> bomComponents = bomList.stream()
            .map(bom -> {
                Item child = bom.getChildItem();
                if(child == null) return null;

                int perParentQty = bom.getQty();
                int totalQty = perParentQty * requestQty;
                int childStockQty = stockByItemCode.getOrDefault(child.getCode(), 0);
                int childShortageQty = Math.max(0, totalQty - childStockQty);
                int childLeadTime = Optional.ofNullable(child.getPurchaseLeadTime()).orElse(0);
                int childSafetyStock = Optional.ofNullable(child.getSafetyStock()).orElse(0);
                int childPurchaseQty = (childShortageQty > 0) ? (childShortageQty + childSafetyStock) : 0;

                return new MrpDetailDto.MrpBomComponent(
                    child.getCode(),
                    child.getName(),
                    perParentQty,
                    totalQty,
                    childStockQty,
                    childShortageQty,
                    childLeadTime,
                    childSafetyStock, // DTO에 추가된 필드
                    childPurchaseQty  // DTO에 추가된 필드
                );
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<ProdOrder> prodOrders = mrpProdOrderRepository.findByPlan_PlanId(plan.getPlanId());
        List<MrpDetailDto.MrpWorkOrder> workOrders = new ArrayList<>();
        for (ProdOrder po : prodOrders) {
            ProdPlan pp = po.getPlan();
            if (pp == null) continue;
            String woNo    = po.getProdOrderId().toString();
            String iCode   = pp.getItem().getCode();
            int qtyWO      = Optional.ofNullable(po.getIssuedQty()).orElse(0);
            String startDt = (pp.getStartDate() != null) ? pp.getStartDate().toString() : "";
            String endDt   = (pp.getDueDate() != null)   ? pp.getDueDate().toString()   : "";
            String stWO    = (pp.getStatus() != null)    ? pp.getStatus().toString()    : "";
            workOrders.add(new MrpDetailDto.MrpWorkOrder(woNo, iCode, qtyWO, startDt, endDt, stWO));
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<InventoryLog> logs = mrpInventoryLogRepository.findByItem_IdOrderByLogDatetimeDesc(parentItem.getId());
        List<MrpDetailDto.MrpHistory> history = new ArrayList<>();
        for (InventoryLog log : logs) {
            String ts  = (log.getLogDatetime() != null) ? log.getLogDatetime().format(dtf) : "";
            String msg = Optional.ofNullable(log.getComment()).orElse("");
            history.add(new MrpDetailDto.MrpHistory(ts, msg));
        }

        return new MrpDetailDto(
            id, createDate, dueDate, planType, status,
            itemCode, itemName, itemType, unit, spec, safetyStock, parentStockQty, location,
            requestQty, shortageQty, purchaseLeadTime, productionLeadTime, orderableDate,
            bomComponents, Collections.emptyList(), workOrders, history
        );
    }

    @Override
    @Transactional
    public void generateMrpForOrder(Long orderId) {
        CompanyOrder order = mrpCompanyOrderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("해당 주문이 없습니다. orderId=" + orderId));
        List<OrderLineItem> lineItems = mrpOrderLineItemRepository.findByCompanyOrder_OrderId(orderId);
        LocalDate today = LocalDate.now();
        for (OrderLineItem oli : lineItems) {
            Item product = oli.getItem();
            int orderQty = Optional.ofNullable(oli.getUnitQty()).orElse(0);
            int productStockQty = stockRepository.findAll().stream().filter(s -> s.getItem() != null && s.getItem().getId().equals(product.getId())).mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0)).sum();
            int shortage = Math.max(orderQty - productStockQty, 0);
            if (shortage <= 0) continue;
            ProdPlan prodPlan = new ProdPlan();
            prodPlan.setItem(product);
            prodPlan.setPlanQty(shortage);
            prodPlan.setUnit(product.getUnit());
            prodPlan.setPlanDate(today);
            prodPlan.setDueDate(order.getOrderDate());
            prodPlan.setStatus(PlanStatus.PLANNED);
            prodPlan.setPriority(1);
            prodPlanRepository.save(prodPlan);
            List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(product);
            for (com.team.berp.domain.Bom bom : bomList) {
                Item component = bom.getChildItem();
                int perParentQty = bom.getQty();
                int totalRequiredQty = perParentQty * shortage;
                int componentStockQty = stockRepository.findAll().stream().filter(s -> s.getItem() != null && s.getItem().getId().equals(component.getId())).mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0)).sum();
                int compShortage = Math.max(totalRequiredQty - componentStockQty, 0);
                Mrp mrp = new Mrp();
                mrp.setPlan(prodPlan);
                mrp.setItem(component);
                mrp.setRequiredQty(totalRequiredQty);
                mrp.setBaseDate(today);
                mrp.setStatus(MrpStatus.PLANNED);
                if (compShortage <= 0) {
                    mrp.setSource("PRODUCTION");
                    mrp.setDueDate(prodPlan.getDueDate());
                } else {
                    int purchaseLeadTime = Optional.ofNullable(component.getPurchaseLeadTime()).orElse(0);
                    mrp.setSource("PURCHASE");
                    mrp.setLeadTime(purchaseLeadTime);
                    mrp.setDueDate(today.plusDays(purchaseLeadTime));
                }
                mrpRepository.save(mrp);
            }
        }
    }

    @Override
    @Transactional
    public void generateMrpForOrderLineItem(Long orderLineItemId, int neededQty) {
        OrderLineItem oli = mrpOrderLineItemRepository.findById(orderLineItemId).orElseThrow(() -> new IllegalArgumentException("주문상세가 없습니다. id=" + orderLineItemId));
        Item product = oli.getItem();
        LocalDate today = LocalDate.now();
        ProdPlan prodPlan = new ProdPlan();
        prodPlan.setItem(product);
        prodPlan.setPlanQty(neededQty);
        prodPlan.setUnit(product.getUnit());
        prodPlan.setPlanDate(today);
        prodPlan.setDueDate(oli.getCompanyOrder().getOrderDate());
        prodPlan.setStatus(PlanStatus.PLANNED);
        prodPlan.setPriority(1);
        prodPlanRepository.save(prodPlan);
        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(product);
        for (com.team.berp.domain.Bom bom : bomList) {
            Item component = bom.getChildItem();
            int perParentQty = bom.getQty();
            int totalRequiredQty = perParentQty * neededQty;
            int componentStockQty = stockRepository.findByItemIdAndQuantityGreaterThan(component.getId(), 0).stream().mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0)).sum();
            int compShortage = Math.max(totalRequiredQty - componentStockQty, 0);
            Mrp mrp = new Mrp();
            mrp.setPlan(prodPlan);
            mrp.setItem(component);
            mrp.setRequiredQty(totalRequiredQty);
            mrp.setBaseDate(today);
            mrp.setStatus(MrpStatus.PLANNED);
            if (compShortage <= 0) {
                mrp.setSource("PRODUCTION");
                mrp.setDueDate(prodPlan.getDueDate());
            } else {
                int purchaseLeadTime = Optional.ofNullable(component.getPurchaseLeadTime()).orElse(0);
                mrp.setSource("PURCHASE");
                mrp.setLeadTime(purchaseLeadTime);
                mrp.setDueDate(today.plusDays(purchaseLeadTime));
            }
            mrpRepository.save(mrp);
        }
    }
}