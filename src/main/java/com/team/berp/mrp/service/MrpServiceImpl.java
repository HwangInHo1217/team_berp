// File: /Team_BERP/src/main/java/com/team/berp/mrp/service/MrpServiceImpl.java
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

    private final EntityMrpRepository        mrpRepository;
    private final EntityItemRepository       itemRepository;
    private final EntityStockRepository      stockRepository;
    private final BomRepository              bomRepository;
    private final EntityProdPlanRepository   prodPlanRepository;

    private final Mrp_OrderLineItemRepository  mrpOrderLineItemRepository;
    private final Mrp_CompanyOrderRepository   mrpCompanyOrderRepository;
    private final Mrp_ProdOrderRepository      mrpProdOrderRepository;
    private final Mrp_InventoryLogRepository   mrpInventoryLogRepository;
    private final Mrp_WarehouseRepository      mrpWarehouseRepository;

    // ──────────────────────────────────────────────────────────
    // (1) 상단 MRP 리스트 조회
    @Override
    public Page<MrpViewDto> findMrpList(
            int page, int size,
            String sortKey, String sortDir,
            String startDate, String endDate,
            String itemSearch
    ) {
        Sort.Direction dir = sortDir.equalsIgnoreCase("asc")
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(dir, sortKey));

        Page<Mrp> mrpPage;
        if ((startDate == null || startDate.isEmpty())
         || (endDate == null || endDate.isEmpty())
         || (itemSearch == null || itemSearch.isEmpty())) {
            mrpPage = mrpRepository.findAll(pageable);
        } else {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end   = LocalDate.parse(endDate);
            mrpPage = mrpRepository
                .findByBaseDateBetweenAndItem_CodeContainingIgnoreCaseOrBaseDateBetweenAndItem_NameContainingIgnoreCase(
                    start, end, itemSearch,
                    start, end, itemSearch,
                    pageable
                );
        }

        // 3) 화면에 뿌릴 DTO 목록 생성
        List<Stock> allStocks = stockRepository.findAll();

        // 3-1) “완제품별로 최신 주문수량(unitQty)” 을 미리 가져와서 맵에 저장
        Map<String, Integer> totalOrderQtyByItemCode = new HashMap<>();
        for (Mrp mrp : mrpPage.getContent()) {
            Item parentItem = mrp.getPlan().getItem();
            if (parentItem == null) continue;

            String code = parentItem.getCode();
            Long itemId = parentItem.getId();

            // **여기서 plan.getPlanQty() 가 아닌, “최신 주문량”을 꺼냅니다**.
            Integer latestOrderQty = mrpRepository.findLatestOrderQtyByItemId(itemId);
            int orderQty = (latestOrderQty != null ? latestOrderQty : 0);

            // 맵에 넣기 (나중에 꺼내 쓸 때, “완제품별 최신 주문량”으로 사용)
            totalOrderQtyByItemCode.put(code, orderQty);
        }

        // 3-2) “완제품별로 중복 제거 & 부족수량 계산 후 (shortageQty>0)만 노출”
        Set<String> seenCodes = new HashSet<>();
        List<MrpViewDto> dtoList = new ArrayList<>();
        for (Mrp mrp : mrpPage.getContent()) {
            Item parentItem = mrp.getPlan().getItem();
            if (parentItem == null) continue;
            String itemCode = parentItem.getCode();
            if (seenCodes.contains(itemCode)) {
                continue;
            }
            seenCodes.add(itemCode);

            // → “완제품 현재고 합산”
            int stockQty = allStocks.stream()
                .filter(s -> s.getItem()!=null && s.getItem().getCode().equals(itemCode))
                .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                .sum();

            // → “완제품 최신 주문수량(orderQty)” ← 맵에서 꺼냄
            int orderQty = totalOrderQtyByItemCode.getOrDefault(itemCode, 0);

            // → 부족수량 계산
            int shortageQty = orderQty - stockQty;

            // “부족수량 <= 0” 이면 화면에서 제외
            if (shortageQty <= 0) {
                continue;
            }

            // — 나머지 DTO 속성
            String itemName = parentItem.getName();
            String itemType = (parentItem.getType() != null
                               ? parentItem.getType().toString()
                               : "");
            String unit     = parentItem.getUnit();
            String spec     = parentItem.getSpec();
            String custName = mrpRepository.findLatestCompanyNameByItemId(parentItem.getId());
            String dueDateStr = (mrp.getDueDate() != null) ? mrp.getDueDate().toString() : "";
            String mrpStatus = (mrp.getStatus() != null)   ? mrp.getStatus().toString()   : "";

            MrpViewDto dto = new MrpViewDto(
                mrp.getMrpId(),
                itemCode,
                itemName,
                itemType,
                unit,
                Optional.ofNullable(mrp.getBaseDate()).map(Object::toString).orElse(""),
                orderQty,
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

    // ──────────────────────────────────────────────────────────
    // (2) BOM(원자재) 리스트 조회
    @Override
    public List<ExtendedBomListViewResponse> findBomByItemCode(String itemCode) {
        if (itemCode == null || itemCode.isEmpty()) {
            return Collections.emptyList();
        }

        // 1) “완제품” 엔티티 조회
        Item parent = itemRepository.findAll().stream()
            .filter(i -> itemCode.equals(i.getCode()))
            .findFirst().orElse(null);
        if (parent == null) {
            return Collections.emptyList();
        }

        // 2) “완제품 최신 주문수량(requestQty)”과 “현재고” 계산
        Integer latestOrderQty = mrpRepository.findLatestOrderQtyByItemId(parent.getId());
        int requestQty = (latestOrderQty != null ? latestOrderQty : 0);

        List<Stock> allStocks = stockRepository.findAll();
        int parentStockQty = allStocks.stream()
            .filter(s -> s.getItem() != null && itemCode.equals(s.getItem().getCode()))
            .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
            .sum();

        int parentShortageQty = Math.max(requestQty - parentStockQty, 0);

        // 3) BOM(부품) 조회
        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(parent);
        LocalDate today = LocalDate.now();

        List<ExtendedComponent> extComps = bomList.stream()
            .map(bom -> {
                Item child = bom.getChildItem();
                int perParentQty = bom.getQty();

                // → “원자재 총 필요 수량(totalQty)”
                int totalQty = perParentQty * parentShortageQty;

                // → “원자재 현재고”
                int childStockQty = allStocks.stream()
                    .filter(s -> s.getItem() != null
                              && child.getCode().equals(s.getItem().getCode()))
                    .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                    .sum();

                // → “원자재 부족수량(shortageQty)”
                int childShortageQty = Math.max(totalQty - childStockQty, 0);

                // → “안전재고”
                int safetyStock = Optional.ofNullable(child.getSafetyStock()).orElse(0);

                // → “총 사용 후 남는 재고” = childStockQty - totalQty
                int remainingAfterUse = childStockQty - totalQty;

                // → “발주필요수량(purchaseQty)”
                int purchaseQty = (remainingAfterUse >= safetyStock)
                                  ? 0
                                  : (safetyStock - remainingAfterUse);

                // → “구매리드타임”
                int purchaseLeadTime = Optional.ofNullable(child.getPurchaseLeadTime()).orElse(0);

                // → “예상입고일”
                String expectedDate = today.plusDays(purchaseLeadTime).toString();

                // → “기본 Component 정보”
                BomListViewResponse.Component base = new BomListViewResponse.Component(
                    child.getCode(),
                    child.getName(),
                    perParentQty,
                    child.getSpec(),
                    child.getUnit(),
                    bom.getSeqNo(),
                    Optional.ofNullable(bom.getLossRt()).map(Object::toString).orElse("0%"),
                    Optional.ofNullable(bom.getItemPrice()).map(String::valueOf).orElse("0"),
                    bom.getRemark()
                );

                return new ExtendedComponent(
                    base,
                    totalQty,
                    childStockQty,
                    childShortageQty,
                    safetyStock,
                    purchaseQty,
                    purchaseLeadTime,
                    expectedDate
                );
            })
            .collect(Collectors.toList());

        ExtendedBomListViewResponse wrapper = new ExtendedBomListViewResponse(
            parent.getCode(),
            parent.getName(),
            extComps
        );
        return Collections.singletonList(wrapper);
    }

    // ──────────────────────────────────────────────────────────
    // (3) MRP 상세 화면 (모달) – ID로 조회
    @Override
    public MrpDetailDto findMrpDetailById(Long mrpId) {
        Mrp mrp = mrpRepository.findById(mrpId).orElse(null);
        if (mrp == null) {
            return null;
        }

        // --- A. 기본 정보 ---
        Long     id           = mrp.getMrpId();
        String   createDate   = (mrp.getBaseDate() != null) ? mrp.getBaseDate().toString() : "";
        String   dueDate      = (mrp.getDueDate()  != null) ? mrp.getDueDate().toString()  : "";
        String   planType     = (mrp.getSource()   != null) ? mrp.getSource()   : "";
        String   status       = (mrp.getStatus() != null)   ? mrp.getStatus().toString()   : "";

        // ───────────────────────────────────────────────────────────
        // (B) “부모 품목(완제품)” 정보를 꺼냅니다.
        ProdPlan plan = mrp.getPlan();
        Item parentItem = (plan != null ? plan.getItem() : null);

        String itemCode    = (parentItem != null ? parentItem.getCode() : "");
        String itemName    = (parentItem != null ? parentItem.getName() : "");
        String itemType    = (parentItem != null && parentItem.getType() != null)
                             ? parentItem.getType().toString() : "";
        String unit        = (parentItem != null ? parentItem.getUnit() : "");
        String spec        = (parentItem != null ? parentItem.getSpec() : "");
        int    safetyStock = (parentItem != null && parentItem.getSafetyStock() != null)
                             ? parentItem.getSafetyStock() : 0;

        // (B2) “완제품” 현재 재고 합산
        List<Stock> allStocks = stockRepository.findAll();
        int parentStockQty = allStocks.stream()
            .filter(s -> s.getItem() != null
                      && itemCode.equals(s.getItem().getCode()))
            .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
            .sum();

        Map<String, Integer> warehouseMap = new LinkedHashMap<>();
        for (Stock s : allStocks) {
            if (s.getItem() != null && itemCode.equals(s.getItem().getCode())) {
                Warehouse wh = s.getWarehouse();
                if (wh != null) {
                    warehouseMap.put(
                        wh.getWarehouseName(),
                        warehouseMap.getOrDefault(wh.getWarehouseName(), 0) + s.getQuantity()
                    );
                }
            }
        }
        String location = warehouseMap.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(e -> String.format("%s(%d)", e.getKey(), e.getValue()))
            .collect(Collectors.joining("  "));

        // ───────────────────────────────────────────────────────────
        // (C) “수량·리드타임” – “최신 주문수량(unitQty)”을 가져오도록 수정
        final int requestQty = (parentItem != null
                ? Optional.ofNullable(mrpRepository.findLatestOrderQtyByItemId(parentItem.getId()))
                          .orElse(0)
                : 0
        );

        int shortageQty = Math.max(requestQty - parentStockQty, 0);

        int purchaseLeadTime = (parentItem != null && parentItem.getPurchaseLeadTime() != null)
                                ? parentItem.getPurchaseLeadTime() : 0;
        int productionLeadTime = Optional.ofNullable(mrp.getLeadTime()).orElse(0);

        long daysToOrderable = Math.min(purchaseLeadTime, productionLeadTime);
        String orderableDate = LocalDate.now().plusDays(daysToOrderable).toString();

        // ───────────────────────────────────────────────────────────
        // (D) BOM 구성: “부모 품목”을 기준으로 계산
        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(parentItem);
        List<MrpDetailDto.MrpBomComponent> bomComponents = bomList.stream()
            .map(bom -> {
                Item child = bom.getChildItem();
                int perParentQty = bom.getQty();
                int totalQty = perParentQty * requestQty;

                int childStockQty = allStocks.stream()
                    .filter(s -> s.getItem() != null 
                              && child.getCode().equals(s.getItem().getCode()))
                    .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                    .sum();

                int childShortageQty = Math.max(totalQty - childStockQty, 0);
                int childLeadTime = Optional.ofNullable(child.getPurchaseLeadTime()).orElse(0);

                return new MrpDetailDto.MrpBomComponent(
                    child.getCode(),
                    child.getName(),
                    perParentQty,
                    totalQty,
                    childStockQty,
                    childShortageQty,
                    childLeadTime
                );
            })
            .collect(Collectors.toList());

        // (E) 생산 오더 리스트
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

            workOrders.add(
                new MrpDetailDto.MrpWorkOrder(woNo, iCode, qtyWO, startDt, endDt, stWO)
            );
        }

        // (F) InventoryLog 이력
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<InventoryLog> logs = mrpInventoryLogRepository.findByItem_IdOrderByLogDatetimeDesc(parentItem.getId());
        List<MrpDetailDto.MrpHistory> history = new ArrayList<>();
        for (InventoryLog log : logs) {
            String ts  = (log.getLogDatetime() != null) 
                         ? log.getLogDatetime().format(dtf) 
                         : "";
            String msg = Optional.ofNullable(log.getComment()).orElse("");
            history.add(new MrpDetailDto.MrpHistory(ts, msg));
        }

        // ───────────────────────────────────────────────────────────
        // (G) 최종 DTO 반환
        return new MrpDetailDto(
            id,                  // mrpId
            createDate,          // createDate
            dueDate,             // dueDate
            planType,            // planType
            status,              // status
            itemCode,            // itemCode
            itemName,            // itemName
            itemType,            // itemType
            unit,                // unit
            spec,                // spec
            safetyStock,         // safetyStock
            parentStockQty,      // stockQty
            location,            // location
            requestQty,          // requiredQty  (이제 실제 주문량)
            shortageQty,         // shortageQty  (5,000−530=4,470)
            purchaseLeadTime,    // purchaseLeadTime
            productionLeadTime,  // productionLeadTime
            orderableDate,       // orderableDate
            bomComponents,       // bomComponents
            Collections.emptyList(), // purchaseOrders
            workOrders,          // workOrders
            history              // history
        );
    }

    // ──────────────────────────────────────────────────────────
    // (4) 새로 추가된 메서드: 주문(orderId)에 따라 ProdPlan과 BOM을 탐색하여 MRP 엔티티 생성 (변경 없음)
    @Override
    @Transactional
    public void generateMrpForOrder(Long orderId) {
        CompanyOrder order = mrpCompanyOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("해당 주문이 없습니다. orderId=" + orderId));
        List<OrderLineItem> lineItems = 
            mrpOrderLineItemRepository.findByCompanyOrder_OrderId(orderId);
        LocalDate today = LocalDate.now();

        for (OrderLineItem oli : lineItems) {
            Item product = oli.getItem();
            int orderQty = Optional.ofNullable(oli.getUnitQty()).orElse(0);

            // 2) 완제품 현재 재고 조회
            int productStockQty = stockRepository.findAll().stream()
                .filter(s -> s.getItem() != null 
                          && s.getItem().getId().equals(product.getId()))
                .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                .sum();

            // 3) 재고 부족량 계산
            int shortage = Math.max(orderQty - productStockQty, 0);
            if (shortage <= 0) {
                // 재고 충분 → MRP 생성 없이 출고 로직(생략)
                continue;
            }

            // 4) ProdPlan 생성
            ProdPlan prodPlan = new ProdPlan();
            prodPlan.setItem(product);
            prodPlan.setPlanQty(shortage);
            prodPlan.setUnit(product.getUnit());
            prodPlan.setPlanDate(today);
            prodPlan.setDueDate(order.getOrderDate());
            prodPlan.setStatus(PlanStatus.PLANNED);
            prodPlan.setPriority(1);
            prodPlanRepository.save(prodPlan);

            // 5) BOM 조회
            List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(product);

            // 6) 각 부품에 대해 MRP 엔티티 생성
            for (com.team.berp.domain.Bom bom : bomList) {
                Item component = bom.getChildItem();
                int perParentQty = bom.getQty();
                int totalRequiredQty = perParentQty * shortage;

                // 부품 현재 재고 조회
                int componentStockQty = stockRepository.findAll().stream()
                    .filter(s -> s.getItem() != null 
                              && s.getItem().getId().equals(component.getId()))
                    .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                    .sum();

                int compShortage = Math.max(totalRequiredQty - componentStockQty, 0);

                // MRP 엔티티 작성
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
}
