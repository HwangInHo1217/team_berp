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
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
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
    @Transactional(readOnly = true)
    public Page<MrpViewDto> findMrpList(
            int page, int size,
            String sortKey, String sortDir,
            String startDate, String endDate,
            String itemSearch
    ) {
        log.info("================== MRP 리스트 조회 시작 ==================");
        log.info("페이지: {}, 시작일: {}, 종료일: {}, 검색어: '{}'", page, startDate, endDate, itemSearch);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "mrpId"));

        boolean noDateFilter = (startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty());
        boolean noSearch = (itemSearch == null || itemSearch.isEmpty());

        List<Mrp> allMatchingMrps;

        if (noSearch) {
            // [수정된 부분] 날짜 필터가 없는 경우(초기 로딩)와 있는 경우를 분리
            if (noDateFilter) {
                log.info("초기 로딩 케이스 실행 (날짜 필터 없음)");
                allMatchingMrps = mrpRepository.findAllByStatus(MrpStatus.PLANNED);
            } else {
                log.info("날짜 단독 검색 케이스 실행");
                LocalDate sd = LocalDate.parse(startDate);
                LocalDate ed = LocalDate.parse(endDate);
                allMatchingMrps = mrpRepository.findAllByStatusAndDueDateBetween(MrpStatus.PLANNED, sd, ed);
            }
        } else {
            log.info("키워드 검색 케이스 실행");
            LocalDate sd = noDateFilter ? LocalDate.MIN : LocalDate.parse(startDate);
            LocalDate ed = noDateFilter ? LocalDate.MAX : LocalDate.parse(endDate);
            allMatchingMrps = mrpRepository.findAllByStatusAndDueDateBetweenAndPlanItemNameOrCode(MrpStatus.PLANNED, sd, ed, itemSearch);
        }
        log.info("[1단계] DB에서 조회된 Mrp 레코드 개수: {}", allMatchingMrps.size());

        // ... (이하 모든 데이터 처리 및 페이지네이션 로직은 이전 답변과 동일하게 유지) ...
        
        // (이전 답변의 3, 4단계 코드와 동일한 내용이 여기에 위치합니다)
        // ...
        List<Stock> allStocks = stockRepository.findAll();
        Map<String, Integer> stockByItemCode = allStocks.stream()
                .filter(s -> s.getItem() != null)
                .collect(Collectors.groupingBy(s -> s.getItem().getCode(), Collectors.summingInt(s -> Optional.ofNullable(s.getQuantity()).orElse(0))));

        Map<String, Integer> totalRequiredByItemCode = new HashMap<>();
        Set<String> processedItemCodesForSum = new HashSet<>();
        log.info("--- [2단계] 품목별 총 필요수량 계산 시작 ---");
        for (Mrp mrp : allMatchingMrps) {
            if (mrp.getPlan() == null || mrp.getPlan().getItem() == null) continue;
            Item parent = mrp.getPlan().getItem();
            String code = parent.getCode();
            if (processedItemCodesForSum.contains(code)) continue;

            Integer sumRequired = mrpRepository.sumRequiredQtyByItemIdAndStatus(parent.getId(), MrpStatus.PLANNED);
            int requiredQty = (sumRequired != null ? sumRequired : 0);
            log.info(" > 품목코드: {}, 계산된 총 필요수량: {}", code, requiredQty);
            totalRequiredByItemCode.put(code, requiredQty);
            processedItemCodesForSum.add(code);
        }
        log.info("--- [2단계] 품목별 총 필요수량 계산 완료 ---");


        List<MrpViewDto> finalDtoList = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        log.info("--- [3단계] 최종 DTO 리스트 생성 시작 ---");
        for (Mrp mrp : allMatchingMrps) {
            if (mrp.getPlan() == null || mrp.getPlan().getItem() == null) continue;
            Item parent = mrp.getPlan().getItem();
            String itemCode = parent.getCode();
            if (seenCodes.contains(itemCode)) continue;
            
            int stockQty = stockByItemCode.getOrDefault(itemCode, 0);
            int requiredQty = totalRequiredByItemCode.getOrDefault(itemCode, 0);
            int shortageQty = requiredQty - stockQty;

            log.info(" > 품목코드: {}, 필요수량: {}, 재고: {}, 부족수량: {}", itemCode, requiredQty, stockQty, shortageQty);

            if (shortageQty <= 0) {
                log.info("   >> 부족수량 0 이하. 리스트에서 제외.");
                continue;
            }
            
            log.info("   >> 부족수량 0 초과. 리스트에 추가!");
            seenCodes.add(itemCode);
            
            MrpViewDto dto = new MrpViewDto(
                    mrp.getMrpId(),
                    itemCode,
                    parent.getName(),
                    (parent.getType() != null ? parent.getType().toString() : ""),
                    parent.getUnit(),
                    Optional.ofNullable(mrp.getBaseDate()).map(Object::toString).orElse(""),
                    requiredQty,
                    stockQty,
                    shortageQty,
                    mrp.getSource(),
                    Optional.ofNullable(mrp.getLeadTime()).orElse(0),
                    mrp.getComment(),
                    mrpRepository.findLatestCompanyNameByItemId(parent.getId()),
                    parent.getSpec(),
                    (mrp.getDueDate() != null ? mrp.getDueDate().toString() : ""),
                    (mrp.getStatus() != null ? mrp.getStatus().toString() : "")
            );
            finalDtoList.add(dto);
        }
        log.info("--- [3단계] 최종 DTO 리스트 생성 완료 ---");
        log.info("[4단계] 화면에 표시될 최종 품목 개수: {}", finalDtoList.size());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), finalDtoList.size());
        
        List<MrpViewDto> pageContent = (start > finalDtoList.size()) ? Collections.emptyList() : finalDtoList.subList(start, end);
        log.info("[5단계] 현재 페이지에 표시될 품목 개수: {}", pageContent.size());
        log.info("================== MRP 리스트 조회 종료 ==================\n");

        return new PageImpl<>(pageContent, pageable, finalDtoList.size());
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

        Integer sumRequired = mrpRepository.sumRequiredQtyByItemIdAndStatus(parentItem.getId(), MrpStatus.PLANNED);
        int parentRequiredQty = (sumRequired != null) ? sumRequired : 0;

        Map<Long, Integer> stockMap = stockRepository.findAll().stream()
                .filter(s -> s.getItem() != null && s.getQuantity() != null)
                .collect(Collectors.groupingBy(s -> s.getItem().getId(),
                                               Collectors.summingInt(Stock::getQuantity)));

        // 여러 버전에 걸친 모든 BOM 자재를 가져옵니다.
        List<com.team.berp.domain.Bom> rawBomList = bomRepository.findByParentItem(parentItem);
        LocalDate today = LocalDate.now();

        // [핵심 수정] DB에서 가져온 BOM 리스트의 중복 자재를 제거합니다.
        Map<Long, com.team.berp.domain.Bom> uniqueBomMap = new LinkedHashMap<>();
        for (com.team.berp.domain.Bom bom : rawBomList) {
            Item childItem = bom.getChildItem();
            if (childItem == null) continue;

            // 맵에 해당 자재가 아직 없으면 추가합니다. (이미 있으면 아무것도 하지 않음)
            // 이렇게 하면 동일한 자재 중 가장 처음 발견된 하나만 남게 됩니다.
            uniqueBomMap.putIfAbsent(childItem.getId(), bom);
        }
        List<com.team.berp.domain.Bom> uniqueBomList = new ArrayList<>(uniqueBomMap.values());
        // [핵심 수정 로직 끝]


        // 중복이 제거된 BOM 리스트(uniqueBomList)를 사용하여 화면에 보낼 데이터를 생성합니다.
        List<ExtendedComponent> extendedComponents = uniqueBomList.stream()
            .map(bom -> {
                Item childItem = bom.getChildItem();
                int perParentQty = bom.getQty(); // 중복 제거된 첫 번째 자재의 소요량
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