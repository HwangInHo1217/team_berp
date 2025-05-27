package com.team.berp.bom.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.bom.dto.AddBomRequestDTO;
import com.team.berp.bom.dto.BomListViewResponse;
import com.team.berp.bom.dto.BomProductItemDTO;
import com.team.berp.bom.dto.ItemSelectionDTO;
import com.team.berp.bom.dto.UpdateBomRequestDTO;
import com.team.berp.bom.repository.BomRepository;
import com.team.berp.domain.Bom;
import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.item.repository.ItemRepository;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
@Service // 스프링 서비스 컴포넌트로 등록
@RequiredArgsConstructor // final 필드를 가진 생성자를 자동 생성
public class BomService {

    private final ItemRepository itemRepository; // 품목(완제품/자재) 관련 JPA Repository
    private final BomRepository bomRepository;   // BOM(구성) 관련 Repository
    //bom 사용 여부로 구분하여 조회
    public Page<BomProductItemDTO> getPagedParentProductList(String field, String keyword, String useYn, Pageable pageable) {
        String searchValue = (keyword != null) ? keyword : "";
        String useValue = (useYn != null && !useYn.isEmpty()) ? useYn : null;

        ItemType type = ItemType.product; // 무조건 제품만 검색 대상

        Page<Item> items;

        if (field == null || field.equals("name")) {
            items = (useValue == null) ?
                itemRepository.findByNameContainingAndType(searchValue, type, pageable) :
                itemRepository.findByNameContainingAndTypeAndUse(searchValue, type, useValue, pageable);
        } else if (field.equals("code")) {
            items = (useValue == null) ?
                itemRepository.findByCodeContainingAndType(searchValue, type, pageable) :
                itemRepository.findByCodeContainingAndTypeAndUse(searchValue, type, useValue, pageable);
        } else {
            items = itemRepository.findByType(type, pageable); // fallback
        }

        return items.map(item -> new BomProductItemDTO(
            item.getId(), item.getCode(), item.getName(),
            item.getSpec(), item.getUnit(), item.getUse(), item.getType()
        ));
    }





    /*
 // ✅ BOM 등록된 완제품 페이징 + 검색
    public Page<BomProductItemDTO> getPagedParentProductList(String keyword, Pageable pageable) {
        return bomRepository.findPagedParentItemsByKeyword(keyword, pageable);
    }*/

    // ✅ 특정 완제품의 BOM 구성 목록 조회
    public BomListViewResponse getBomByParentItemId(Long parentId) {
        Item parent = itemRepository.findById(parentId) // parent item 조회
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품목입니다."));

        List<Bom> bomList = bomRepository.findByParentItemId(parentId); // 해당 품목의 BOM 구성 조회

        List<BomListViewResponse.Component> components = new ArrayList<>();
        for (Bom bom : bomList) { // BOM 엔티티 → DTO로 변환
            components.add(new BomListViewResponse.Component(
                bom.getChildItem().getCode(),
                bom.getChildItem().getName(),
                bom.getQty(),
                bom.getChildItem().getSpec(), // 규격
                bom.getChildItem().getUnit(), // 단위
                bom.getSeqNo(),
                formatLossRate(bom.getLossRt()), // "5%" 포맷
                bom.getItemPrice() != null ? String.valueOf(bom.getItemPrice()) : "-",
                bom.getRemark() != null ? bom.getRemark() : "-"
            ));
        }

        return new BomListViewResponse( // 최종 응답 DTO 생성
            parent.getCode(),
            parent.getName(),
            components
        );
    }

    // ✅ 로스율을 "10%" 형태로 가공
    private String formatLossRate(BigDecimal lossRt) {
        if (lossRt == null) return "-";
        return lossRt.stripTrailingZeros().toPlainString() + "%";
    }

    // ✅ BOM 등록된 완제품 목록 조회
    public List<BomProductItemDTO> getParentProductDTOList() {
        List<Item> items = bomRepository.findDistinctParentItems(); // 중복 없이 parent item 조회
        List<BomProductItemDTO> dtoList = new ArrayList<>();

        for (Item item : items) {
            BomProductItemDTO dto = new BomProductItemDTO(
                item.getId(),
                item.getCode(),
                item.getName(),
                item.getSpec(),
                item.getUnit(),
                item.getUse(),
                item.getType()
            );
            dtoList.add(dto);
        }

        return dtoList;
    }

    // ✅ BOM 등록 기능 (완제품 + 구성 자재들 저장)
    @Transactional
    public void addBom(AddBomRequestDTO dto) {
        Item parent = itemRepository.findById(dto.getParentItemId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 완제품 ID입니다."));

        for (AddBomRequestDTO.BomComponent comp : dto.getComponents()) {
            Item child = itemRepository.findById(comp.getChildItemId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자재 ID입니다."));

            Bom bom = new Bom(); // 새 BOM 객체 생성 및 세팅
            bom.setParentItem(parent);
            bom.setChildItem(child);
            bom.setQty(comp.getQty());
            bom.setSeqNo(comp.getSeqNo());
            bom.setLossRt(comp.getLossRt() != null ? BigDecimal.valueOf(comp.getLossRt()) : null);
            bom.setItemPrice(comp.getItemPrice());
            bom.setRemark(comp.getRemark());

            bomRepository.save(bom); // 저장
        }
    }

    // ✅ BOM 등록용 완제품/자재 선택 리스트
    public ItemSelectionDTO getSelectableItems() {
        return new ItemSelectionDTO(
            itemRepository.findByType(ItemType.product), // 완제품
            itemRepository.findByType(ItemType.raw)     // 자재
        );
    }
    
    // ✅ 완제품별로 BOM 구성 목록 그룹화
    public List<BomListViewResponse> getBomGroupedByParent() {
        List<Item> parents = itemRepository.findByType(ItemType.product); // 완제품 리스트
        List<BomListViewResponse> result = new ArrayList<>();

        for (Item parent : parents) {
            List<Bom> boms = bomRepository.findByParentItem(parent); // BOM 리스트

            List<BomListViewResponse.Component> components = new ArrayList<>();
            for (Bom b : boms) {
                components.add(new BomListViewResponse.Component(
                    b.getChildItem().getCode(),
                    b.getChildItem().getName(),
                    b.getQty(),
                    b.getChildItem().getSpec(),
                    b.getChildItem().getUnit(),
                    b.getSeqNo(),
                    formatLossRate(b.getLossRt()),
                    b.getItemPrice() != null ? String.valueOf(b.getItemPrice()) : "-",
                    b.getRemark() != null ? b.getRemark() : "-"
                ));
            }

            BomListViewResponse dto = new BomListViewResponse(
                parent.getCode(),
                parent.getName(),
                components
            );
            result.add(dto);
        }

        return result;
    }

    // ✅ BOM 수정 기능 - 기존 BOM 삭제 후 재등록
    @Transactional
    public void updateBom(UpdateBomRequestDTO dto) {
        bomRepository.deleteByParentItemId(dto.getParentItemId()); // 기존 BOM 삭제

        for (UpdateBomRequestDTO.BomComponent c : dto.getComponents()) {
            Bom bom = new Bom();
            bom.setParentItem(itemRepository.findById(dto.getParentItemId()).orElseThrow());
            bom.setChildItem(itemRepository.findById(c.getChildItemId()).orElseThrow());
            bom.setQty(c.getQty());

            bomRepository.save(bom); // 새 구성 등록
        }
    }

    // ✅ BOM 일괄 삭제 (완제품 기준)
    @Transactional
    public void deleteBomsByParentIds(List<Long> parentIds) {
        for (Long parentId : parentIds) {
            bomRepository.deleteByParentItemId(parentId);
        }
    }

    
}
