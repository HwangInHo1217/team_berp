// File: /Team_BERP/src/main/java/com/team/berp/mrp/service/MrpServiceImpl.java
package com.team.berp.mrp.service;

import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.Item;
import com.team.berp.domain.Mrp;
import com.team.berp.domain.MrpStatus;                  // MrpStatus enum
import com.team.berp.domain.OrderLineItem;
import com.team.berp.domain.ProdPlan;
import com.team.berp.domain.ProdPlan.PlanStatus;        // ProdPlan 내부 enum
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
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(dir, sortKey));

        Page<Mrp> mrpPage;
        if ((startDate == null || startDate.isEmpty())
         || (endDate   == null || endDate.isEmpty())
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

        List<Stock> allStocks = stockRepository.findAll();
        List<MrpViewDto> dtoList = mrpPage.getContent().stream()
                .map(mrp -> {
                    Item item = mrp.getItem();

                    // 1) 현재고 합산
                    int stockQty = allStocks.stream()
                        .filter(s -> s.getItem() != null
                                  && s.getItem().getCode().equals(item.getCode()))
                        .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                        .sum();

                    // 2) 주문수량: ProdPlan.planQty
                    int orderQty = Optional.ofNullable(mrp.getPlan().getPlanQty()).orElse(0);

                    // 3) 부족수량 계산
                    int required     = Optional.ofNullable(mrp.getRequiredQty()).orElse(0);
                    int shortageQty  = Math.max(required - stockQty, 0);

                    // 4) 기타 부가 정보
                    String custName  = mrpRepository.findLatestCompanyNameByItemId(item.getId());
                    String spec      = item.getSpec();
                    String dueDateStr = (mrp.getDueDate() != null) ? mrp.getDueDate().toString() : "";
                    String mrpStatus = (mrp.getStatus() != null) ? mrp.getStatus().toString() : "";


                return new MrpViewDto(
                       mrp.getMrpId(),
                       item.getCode(),
                       item.getName(),
                       item.getType().toString(),
                       item.getUnit(),
                       Optional.ofNullable(mrp.getBaseDate()).map(Object::toString).orElse(""),

                       orderQty,           // (변경) 주문수량
                       stockQty,           // 현재고
                       shortageQty,        // 부족수량

                       mrp.getSource(),
                       Optional.ofNullable(mrp.getLeadTime()).orElse(0),
                       mrp.getComment(),

                       custName,
                       spec,

                       dueDateStr,
                       mrpStatus
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, mrpPage.getTotalElements());
    }

    // ──────────────────────────────────────────────────────────
    // (2) BOM(원자재) 리스트 조회
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

        List<ExtendedComponent> extComps = bomList.stream()
            .map(bom -> {
                Item child = bom.getChildItem();
                int requiredQty = bom.getQty();

                int stockQty = allStocks.stream()
                    .filter(s -> s.getItem() != null
                              && s.getItem().getCode().equals(child.getCode()))
                    .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                    .sum();

                int shortageQty = Math.max(requiredQty - stockQty, 0);
                int safetyStock = Optional.ofNullable(child.getSafetyStock()).orElse(0);
                int remainingAfterUse = stockQty - requiredQty;
                int purchaseQty = (remainingAfterUse >= safetyStock)
                                  ? 0
                                  : (safetyStock - remainingAfterUse);
                int purchaseLeadTime = Optional.ofNullable(child.getPurchaseLeadTime()).orElse(0);
                String expectedDate = today.plusDays(purchaseLeadTime).toString();

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

        // --- B. 품목 정보 ---
        Item item = mrp.getItem();
        String itemCode     = (item != null ? item.getCode() : "");
        String itemName     = (item != null ? item.getName() : "");
        String itemType     = (item != null && item.getType() != null) ? item.getType().toString() : "";
        String unit         = (item != null ? item.getUnit() : "");
        String spec         = (item != null ? item.getSpec() : "");
        int    safetyStock  = (item != null && item.getSafetyStock() != null) 
                                ? item.getSafetyStock() 
                                : 0;

        // --- 전체 재고(Stock) 합산 & 재고 위치 문자열 생성 ---
        List<Stock> allStocks = stockRepository.findAll();
        int stockQty = allStocks.stream()
            .filter(s -> s.getItem() != null
                      && itemCode.equals(s.getItem().getCode()))
            .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
            .sum();

        // 재고 위치: “창고A(수량), 창고B(수량)” 식으로
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
            .collect(Collectors.joining("  ")); // 띄어쓰기 두 칸 구분

        // --- C. 수량·리드타임 ---
        int requiredQty       = Optional.ofNullable(mrp.getRequiredQty()).orElse(0);
        int shortageQty       = Math.max(requiredQty - stockQty, 0);
        int purchaseLeadTime  = (item != null && item.getPurchaseLeadTime() != null)
                                  ? item.getPurchaseLeadTime()
                                  : 0;
        int productionLeadTime= Optional.ofNullable(mrp.getLeadTime()).orElse(0);

        long daysToOrderable = Math.min(purchaseLeadTime, productionLeadTime);
        String orderableDate = LocalDate.now().plusDays(daysToOrderable).toString();

        // --- D. BOM 구성 ---
        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(item);
        List<MrpDetailDto.MrpBomComponent> bomComponents = bomList.stream()
            .map(bom -> {
                Item child = bom.getChildItem();
                int perParentQty = bom.getQty();
                int totalQty     = perParentQty * requiredQty;

                // 자재(Child) 현재고
                int childStockQty = allStocks.stream()
                    .filter(s -> s.getItem() != null 
                              && child.getCode().equals(s.getItem().getCode()))
                    .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                    .sum();

                int childShortageQty = Math.max(totalQty - childStockQty, 0);
                int childLeadTime    = Optional.ofNullable(child.getPurchaseLeadTime()).orElse(0);

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

        // --- E. 연계 오더 현황: 구매 오더(PO) 리스트 삭제 ---
        List<MrpDetailDto.MrpPurchaseOrder> purchaseOrders = Collections.emptyList();

        // --- E. 연계 오더 현황: 생산 오더(WO) 리스트 ---
        List<ProdOrder> prodOrders = 
            mrpProdOrderRepository.findByPlan_PlanId(mrp.getPlan().getPlanId());
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

        // --- F. 스케줄·이력(InventoryLog) ---
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<InventoryLog> logs = 
            mrpInventoryLogRepository.findByItem_IdOrderByLogDatetimeDesc(item.getId());
        List<MrpDetailDto.MrpHistory> history = new ArrayList<>();
        for (InventoryLog log : logs) {
            String ts  = (log.getLogDatetime() != null) 
                         ? log.getLogDatetime().format(dtf) 
                         : "";
            String msg = Optional.ofNullable(log.getComment()).orElse("");
            history.add(new MrpDetailDto.MrpHistory(ts, msg));
        }

        // ─── 최종 DTO 반환 ────────────────────────────────────────
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
            stockQty,            // stockQty
            location,            // location
            requiredQty,         // requiredQty
            shortageQty,         // shortageQty
            purchaseLeadTime,    // purchaseLeadTime
            productionLeadTime,  // productionLeadTime
            orderableDate,       // orderableDate
            bomComponents,       // bomComponents
            purchaseOrders,      // purchaseOrders
            workOrders,          // workOrders
            history              // history
        );
    }

    // ──────────────────────────────────────────────────────────
    // (4) 새로 추가된 메서드: 주문(orderId)에 따라 ProdPlan과 BOM을 탐색하여 MRP 레코드 자동 생성
    @Override
    @Transactional
    public void generateMrpForOrder(Long orderId) {
        // 1) CompanyOrder 조회
        CompanyOrder order = mrpCompanyOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("해당 주문이 없습니다. orderId=" + orderId));

        // ─── 변경된 부분: findByOrder_OrderId → findByCompanyOrder_OrderId ───
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

            // 4) ProdPlan(생산 계획) 생성
            ProdPlan prodPlan = new ProdPlan();
            prodPlan.setItem(product);
            prodPlan.setPlanQty(shortage);
            prodPlan.setUnit(product.getUnit());
            prodPlan.setPlanDate(today);
            prodPlan.setDueDate(order.getOrderDate());
            // 내부 enum PlanStatus 사용
            prodPlan.setStatus(PlanStatus.PLANNED);
            prodPlan.setPriority(1);
            prodPlanRepository.save(prodPlan);

            // 5) BOM(부품 구성) 조회
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

                // 새로운 Mrp 엔티티 구성
                Mrp mrp = new Mrp();
                mrp.setPlan(prodPlan);
                mrp.setItem(component);
                mrp.setRequiredQty(totalRequiredQty);
                mrp.setBaseDate(today);
                // 내부 enum MrpStatus 사용
                mrp.setStatus(MrpStatus.PLANNED);

                if (compShortage <= 0) {
                    // 부품 재고 충분 → 생산(Production) 소요
                    mrp.setSource("PRODUCTION");
                    mrp.setDueDate(prodPlan.getDueDate());
                } else {
                    // 부품 재고 부족 → 발주(Purchase) 소요
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
