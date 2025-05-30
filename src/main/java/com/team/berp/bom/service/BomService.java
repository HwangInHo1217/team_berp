// File: src/main/java/com/team/berp/bom/service/BomService.java
package com.team.berp.bom.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.bom.dto.AddBomRequestDTO;
import com.team.berp.bom.dto.BomListViewResponse;
import com.team.berp.bom.dto.BomProductItemDTO;
import com.team.berp.bom.dto.BomVersionResponseDTO;
import com.team.berp.bom.dto.ItemSelectionDTO;
import com.team.berp.bom.dto.UpdateBomRequestDTO;
import com.team.berp.bom.repository.BomRepository;
import com.team.berp.bom.repository.BomVersionRepository;
import com.team.berp.domain.Bom;
import com.team.berp.domain.BomVersion;
import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.mrp.repository.EntityStockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BomService {

    private final ItemRepository        itemRepository;
    private final BomRepository         bomRepository;
    private final BomVersionRepository  bomVersionRepository;
    private final EntityStockRepository stockRepository;

    /**
     * 1) 단일 BOM 버전 상세 조회
     */
    public BomListViewResponse getBomByVersionId(Long versionId) {
        BomVersion version = bomVersionRepository.findById(versionId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 BOM 버전입니다. id=" + versionId));

        List<BomListViewResponse.Component> comps = new ArrayList<>();
        List<Bom> bomList = bomRepository.findByBomVersion(version);
        List<com.team.berp.domain.Stock> allStocks = stockRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Bom b : bomList) {
            Item child = b.getChildItem();
            int stockQty = allStocks.stream()
                .filter(s -> s.getItem().getCode().equals(child.getCode()))
                .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0)
                .sum();
            int shortageQty      = Math.max(b.getQty() - stockQty, 0);
            int safetyStock      = child.getSafetyStock() != null ? child.getSafetyStock() : 0;
            int purchaseQty      = shortageQty + safetyStock;
            int purchaseLeadTime = child.getPurchaseLeadTime() != null ? child.getPurchaseLeadTime() : 0;
            String expectedDate  = today.plusDays(purchaseLeadTime).toString();

            comps.add(new BomListViewResponse.Component(
                child.getCode(),
                child.getName(),
                b.getQty(),
                child.getSpec(),
                child.getUnit(),
                b.getSeqNo(),
                formatLossRate(b.getLossRt()),
                b.getItemPrice() != null ? b.getItemPrice().toString() : "-",
                b.getRemark()    != null ? b.getRemark()           : "-",
                stockQty,
                shortageQty,
                safetyStock,
                purchaseQty,
                purchaseLeadTime,
                expectedDate
            ));
        }

        return new BomListViewResponse(
            version.getParentItem().getCode(),
            version.getParentItem().getName(),
            comps
        );
    }

    /**
     * 2) 특정 완제품의 모든 BOM 버전 목록 조회
     */
    public List<BomVersionResponseDTO> getVersionsByParentId(Long parentId) {
        Item parent = itemRepository.findById(parentId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 완제품입니다. id=" + parentId));

        List<BomVersionResponseDTO> dtoList = new ArrayList<>();
        for (BomVersion v : bomVersionRepository.findByParentItem(parent)) {
            dtoList.add(new BomVersionResponseDTO(v.getId(), v.getVersionCode(), v.getUseYn()));
        }
        return dtoList;
    }

    /**
     * 3) 페이징 + 검색된 완제품 리스트 조회
     */
    public Page<BomProductItemDTO> getPagedParentProductList(
            String searchField,
            String keyword,
            String useYn,
            Pageable pageable
    ) {
        String sval = (keyword != null) ? keyword : "";
        String uval = (useYn != null && !useYn.isEmpty()) ? useYn : null;
        ItemType type = ItemType.product;

        Page<Item> page;
        if ("code".equalsIgnoreCase(searchField)) {
            page = (uval == null)
                ? itemRepository.findByCodeContainingAndType(sval, type, pageable)
                : itemRepository.findByCodeContainingAndTypeAndUse(sval, type, uval, pageable);
        } else {
            page = (uval == null)
                ? itemRepository.findByNameContainingAndType(sval, type, pageable)
                : itemRepository.findByNameContainingAndTypeAndUse(sval, type, uval, pageable);
        }

        return page.map(i -> new BomProductItemDTO(
            i.getId(),
            i.getCode(),
            i.getName(),
            i.getSpec(),
            i.getUnit(),
            i.getUse(),
            i.getType()
        ));
    }

    /**
     * 4) 신규 버전 + BOM 구성 등록
     */
    @Transactional
    public void registerBomWithVersion(AddBomRequestDTO dto) {
        Item parent = itemRepository.findById(dto.getParentItemId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 완제품입니다. id=" + dto.getParentItemId()));

        BomVersion version = BomVersion.builder()
            .versionCode(dto.getVersionCode())
            .description(dto.getDescription())
            .useYn(dto.getUseYn())
            .parentItem(parent)
            .build();
        try {
            bomVersionRepository.save(version);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("중복된 BOM 버전 코드입니다.");
        }

        List<Bom> toSave = new ArrayList<>();
        dto.getComponents().forEach(c -> {
            Item child = itemRepository.findById(c.getChildItemId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자재입니다. id=" + c.getChildItemId()));
            toSave.add(Bom.builder()
                .parentItem(parent)
                .bomVersion(version)
                .childItem(child)
                .qty(c.getQty())
                .seqNo(c.getSeqNo())
                .lossRt(c.getLossRt())
                .itemPrice(c.getItemPrice())
                .remark(c.getRemark())
                .build()
            );
        });
        bomRepository.saveAll(toSave);
    }

    /**
     * 5) 기존 BOM 삭제 후 재등록
     */
    @Transactional
    public void updateBom(UpdateBomRequestDTO dto) {
        bomRepository.deleteByParentItemId(dto.getParentItemId());
        Item parent = itemRepository.findById(dto.getParentItemId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 완제품입니다. id=" + dto.getParentItemId()));
        dto.getComponents().forEach(c -> {
            Item child = itemRepository.findById(c.getChildItemId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자재입니다. id=" + c.getChildItemId()));
            bomRepository.save(Bom.builder()
                .parentItem(parent)
                .childItem(child)
                .qty(c.getQty())
                .seqNo(c.getSeqNo())
                .build()
            );
        });
    }

    /**
     * 6) 여러 완제품 ID로 BOM 일괄 삭제
     */
    @Transactional
    public void deleteBomsByParentIds(List<Long> parentIds) {
        parentIds.forEach(bomRepository::deleteByParentItemId);
    }

    /**
     * 7) 화면용 완제품/자재 선택 리스트
     */
    public ItemSelectionDTO getSelectableItems() {
        List<Item> products = itemRepository.findByType(ItemType.product);
        List<Item> raws     = itemRepository.findByType(ItemType.raw);
        return new ItemSelectionDTO(products, raws);
    }

    // 손실률 → "5%" 포맷
    private String formatLossRate(BigDecimal lossRt) {
        if (lossRt == null) return "-";
        return lossRt.stripTrailingZeros().toPlainString() + "%";
    }
}
