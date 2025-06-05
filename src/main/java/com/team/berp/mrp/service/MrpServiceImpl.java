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
    // (1) MRP 리스트 조회
    @Override
    public Page<MrpViewDto> findMrpList(
            int page, int size,
            String sortKey, String sortDir,
            String startDate, String endDate,
            String itemSearch
    ) {
        // ───────────────────────────────────────────────────────────
        // 1) 정렬(asc/desc)과 페이징 객체 생성
        Sort.Direction dir = sortDir.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(dir, sortKey));

        // ───────────────────────────────────────────────────────────
        // 2) “기간(From/To)”이 비어 있으면, 최솟값~최댓값으로 세팅
        LocalDate start = (startDate == null || startDate.isEmpty())
                ? LocalDate.MIN
                : LocalDate.parse(startDate);
        LocalDate end   = (endDate   == null || endDate.isEmpty())
                ? LocalDate.MAX
                : LocalDate.parse(endDate);

        // ───────────────────────────────────────────────────────────
        // 3) MRP 엔티티 조회 (완제품 검색/기간 필터 적용)
        Page<Mrp> mrpPage;
        if ((startDate == null || startDate.isEmpty()) ||
            (endDate   == null || endDate.isEmpty()) ||
            (itemSearch == null || itemSearch.isEmpty())) {
            // 조건이 하나라도 비어 있으면 단순히 전체 조회 (페이징/정렬만)
            mrpPage = mrpRepository.findAll(pageable);
        } else {
            // “완제품(itemSearch) + 기간” 필터
            mrpPage = mrpRepository
                    .findByBaseDateBetweenAndPlan_Item_CodeContainingIgnoreCaseOrBaseDateBetweenAndPlan_Item_NameContainingIgnoreCase(
                            start, end, itemSearch,
                            start, end, itemSearch,
                            pageable
                    );
        }

        // ───────────────────────────────────────────────────────────
        // 4) 현재고(Stock) 정보를 한 번에 조회해서 Map<제품코드, 재고합계> 형태로 저장
        List<Stock> allStocks = stockRepository.findAll();
        Map<String, Integer> stockByItemCode = allStocks.stream()
                .filter(s -> s.getItem() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getItem().getCode(),
                        Collectors.summingInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                ));

        // ───────────────────────────────────────────────────────────
        // 5) 제품별 MRP 필요수량(requiredQty) 합계를 구하는 Map 생성
        //    Map의 key = 완제품 코드(String), value = MRP 필요수량 합계(Integer)
        Map<String, Integer> totalRequiredByItemCode = new HashMap<>();
        for (Mrp mrp : mrpPage.getContent()) {
            // ◀ 여기서 “완제품”을 꺼내야 합니다.
            Item parent = mrp.getPlan().getItem();
            if (parent == null) continue;

            Long itemId = parent.getId();
            String code = parent.getCode();

            // “PLANNED” 상태인 모든 MRP(requiredQty) 합계를 꺼냄
            Integer sumRequired = mrpRepository.sumRequiredQtyByItemIdAndStatus(
                    itemId, MrpStatus.PLANNED);
            int requiredQty = (sumRequired != null ? sumRequired : 0);

            totalRequiredByItemCode.put(code, requiredQty);
        }

        // ───────────────────────────────────────────────────────────
        // 6) 중복된 제품 코드를 제거하면서 DTO 생성
        Set<String> seenCodes = new HashSet<>();
        List<MrpViewDto> dtoList = new ArrayList<>();

        for (Mrp mrp : mrpPage.getContent()) {
            // ◀ 반드시 “완제품 parent”를 꺼냅니다.
            Item parent = mrp.getPlan().getItem();
            if (parent == null) continue;

            String itemCode = parent.getCode();
            if (seenCodes.contains(itemCode)) {
                // 이미 한 번 처리한 제품코드는 건너뜀
                continue;
            }
            seenCodes.add(itemCode);

            // 6-1) ‘제품 현재고’ 꺼내기
            int stockQty = stockByItemCode.getOrDefault(itemCode, 0);

            // 6-2) ‘제품별 MRP 필요수량(requiredQty)’ 꺼내기
            int requiredQty = totalRequiredByItemCode.getOrDefault(itemCode, 0);

            // 6-3) 부족수량 계산
            int shortageQty = requiredQty - stockQty;
            if (shortageQty <= 0) {
                // 재고가 충분하면 화면에서 제외
                continue;
            }

            // ───────────── 나머지 DTO 항목들 세팅 ─────────────
            String itemName   = parent.getName();   // 완제품명
            String itemType   = (parent.getType() != null ? parent.getType().toString() : "");
            String unit       = parent.getUnit();
            String spec       = parent.getSpec();
            // “가장 최근 거래처명”은 기존 메서드를 그대로 활용 (단, parent.getId())
            String custName   = mrpRepository.findLatestCompanyNameByItemId(parent.getId());
            String dueDateStr = (mrp.getDueDate() != null ? mrp.getDueDate().toString() : "");
            String mrpStatus  = (mrp.getStatus() != null ? mrp.getStatus().toString() : "");

            // 6-4) DTO 생성
            MrpViewDto dto = new MrpViewDto(
                    mrp.getMrpId(),                       // MRP ID
                    itemCode,                             // 완제품 코드
                    itemName,                             // 완제품명
                    itemType,                             // 완제품 유형
                    unit,                                 // 단위
                    Optional.ofNullable(mrp.getBaseDate())
                            .map(Object::toString).orElse(""), // 기준 일자
                    requiredQty,                          // ◀ 합산된 수량 (예: 7110)
                    stockQty,                             // ◀ 현재고 (예: 620)
                    shortageQty,                          // ◀ 부족수량 (예: 6490)
                    mrp.getSource(),                      // 소스(PURCHASE/PRODUCTION)
                    Optional.ofNullable(mrp.getLeadTime()).orElse(0), // 리드타임
                    mrp.getComment(),                     // 코멘트
                    custName,                             // 가장 최근 거래처명
                    spec,                                 // 사양
                    dueDateStr,                           // 납기 요청일
                    mrpStatus                             // MRP 상태(PLANNED)
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
                    .filter(s -> s.getItem() != null && child.getCode().equals(s.getItem().getCode()))
                    .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                    .sum();

                // → “원자재 부족수량(shortageQty)”
                int childShortageQty = Math.max(totalQty - childStockQty, 0);

                // → “안전재고”
                int safetyStock = Optional.ofNullable(child.getSafetyStock()).orElse(0);

                // → “총 사용 후 남는 재고” = childStockQty - totalQty
                int remainingAfterUse = childStockQty - totalQty;

                // → “발주필요수량(purchaseQty)”
                int purchaseQty = (remainingAfterUse >= safetyStock) ? 0 : (safetyStock - remainingAfterUse);

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
            .filter(s -> s.getItem() != null && itemCode.equals(s.getItem().getCode()))
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
           // → 메인 리스트와 동일하게 PLANNED 상태의 MRP(requiredQty)를 모두 합산해서 요청수량으로 사용
        Integer sumRequired = (parentItem != null
            ? mrpRepository.sumRequiredQtyByItemIdAndStatus(parentItem.getId(), MrpStatus.PLANNED)
            : 0
        );
        final int requestQty = (sumRequired != null ? sumRequired : 0);

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
                    .filter(s -> s.getItem() != null && child.getCode().equals(s.getItem().getCode()))
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
            requestQty,          // requiredQty
            shortageQty,         // shortageQty
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
    // (4) (기존) 주문 전체에 대해 MRP 생성
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

    // ──────────────────────────────────────────────────────────
    // (5) (추가) 단일 OrderLineItem 기준으로 부족량만큼 MRP 생성
    @Override
    @Transactional
    public void generateMrpForOrderLineItem(Long orderLineItemId, int neededQty) {
        // 1) OrderLineItem 엔티티 조회
        OrderLineItem oli = mrpOrderLineItemRepository.findById(orderLineItemId)
            .orElseThrow(() -> new IllegalArgumentException("주문상세가 없습니다. id=" + orderLineItemId));

        Item product = oli.getItem();
        LocalDate today = LocalDate.now();

        // 2) ProdPlan 생성 (완제품 기준)
        ProdPlan prodPlan = new ProdPlan();
        prodPlan.setItem(product);
        prodPlan.setPlanQty(neededQty);
        prodPlan.setUnit(product.getUnit());
        prodPlan.setPlanDate(today);
        prodPlan.setDueDate(oli.getCompanyOrder().getOrderDate()); // 주문 납기일을 ProdPlan 납기로 사용
        prodPlan.setStatus(PlanStatus.PLANNED);
        prodPlan.setPriority(1);
        prodPlanRepository.save(prodPlan);

        // 3) 해당 완제품의 BOM(원자재) 조회
        List<com.team.berp.domain.Bom> bomList = bomRepository.findByParentItem(product);

        // 4) 각 원자재별로 MRP 엔티티 생성
        for (com.team.berp.domain.Bom bom : bomList) {
            Item component = bom.getChildItem();
            int perParentQty = bom.getQty();                   // 모품 1대당 필요한 자재 수량
            int totalRequiredQty = perParentQty * neededQty;   // 예: perParentQty=1, neededQty=70 → 70

            // 4-1) 원자재 현재 재고 조회
            int componentStockQty = stockRepository.findByItemIdAndQuantityGreaterThan(
                    component.getId(), 0
                ).stream()
                .mapToInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))
                .sum();

            int compShortage = Math.max(totalRequiredQty - componentStockQty, 0);

            // 4-2) MRP 엔티티 생성 & 저장
            Mrp mrp = new Mrp();
            mrp.setPlan(prodPlan);
            mrp.setItem(component);
            mrp.setRequiredQty(totalRequiredQty);
            mrp.setBaseDate(today);
            mrp.setStatus(MrpStatus.PLANNED);

            if (compShortage <= 0) {
                // 재고 충분 → “생산(Production)” 소요처
                mrp.setSource("PRODUCTION");
                mrp.setDueDate(prodPlan.getDueDate());
            } else {
                // 재고 부족 → “발주(Purchase)” 소요처
                int purchaseLeadTime = Optional.ofNullable(component.getPurchaseLeadTime()).orElse(0);
                mrp.setSource("PURCHASE");
                mrp.setLeadTime(purchaseLeadTime);
                mrp.setDueDate(today.plusDays(purchaseLeadTime));
            }

            mrpRepository.save(mrp);
        }
    }
}
