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

@Service
@RequiredArgsConstructor
public class BomService {
    private final ItemRepository itemRepository;
    private final BomRepository bomRepository;
    
    public BomListViewResponse getBomByParentItemId(Long parentId) {
        Item parent = itemRepository.findById(parentId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 품목입니다."));

        List<Bom> bomList = bomRepository.findByParentItemId(parentId);

        List<BomListViewResponse.Component> components = new ArrayList<>();
        for (Bom bom : bomList) {
            components.add(new BomListViewResponse.Component(
                bom.getChildItem().getCode(),
                bom.getChildItem().getName(),
                bom.getQty()
            ));
        }

        return new BomListViewResponse(
            parent.getCode(),
            parent.getName(),
            components
        );
    }

   
    public List<BomProductItemDTO> getParentProductDTOList() {
        List<Item> items = bomRepository.findDistinctParentItems();
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
    
    @Transactional
    public void addBom(AddBomRequestDTO dto) {
        // 완제품 조회
        Item parent = itemRepository.findById(dto.getParentItemId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 완제품 ID입니다."));

        // 자재 구성 등록
        for (AddBomRequestDTO.BomComponent comp : dto.getComponents()) {
            Item child = itemRepository.findById(comp.getChildItemId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자재 ID입니다."));

            Bom bom = new Bom();
            bom.setParentItem(parent);
            bom.setChildItem(child);
            bom.setQty(comp.getQty());
            bom.setSeqNo(comp.getSeqNo());
            bom.setLossRt(comp.getLossRt() != null ? BigDecimal.valueOf(comp.getLossRt()) : null);
            bom.setItemPrice(comp.getItemPrice());
            bom.setRemark(comp.getRemark());
            bomRepository.save(bom);
        }
    }
    public ItemSelectionDTO getSelectableItems() { 
        return new ItemSelectionDTO(
            itemRepository.findByType(ItemType.product),
            itemRepository.findByType(ItemType.raw)
        );
    }
    public List<BomListViewResponse> getBomGroupedByParent() {
    	 List<Item> parents = itemRepository.findByType(ItemType.product); // 완제품만 조회
    	    List<BomListViewResponse> result = new ArrayList<>();

    	    for (Item parent : parents) {
    	        List<Bom> boms = bomRepository.findByParentItem(parent);

    	        List<BomListViewResponse.Component> components = new ArrayList<>();
    	        for (Bom b : boms) {
    	        	BomListViewResponse.Component component = new BomListViewResponse.Component(
    	                b.getChildItem().getCode(),
    	                b.getChildItem().getName(),
    	                b.getQty()
    	            );
    	            components.add(component);
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
    @Transactional
    public void updateBom(UpdateBomRequestDTO dto) {
        // 기존 BOM 삭제
        bomRepository.deleteByParentItemId(dto.getParentItemId());

        // 새로 삽입
        for (UpdateBomRequestDTO.BomComponent c : dto.getComponents()) {
            Bom bom = new Bom();
            bom.setParentItem(itemRepository.findById(dto.getParentItemId()).orElseThrow());
            bom.setChildItem(itemRepository.findById(c.getChildItemId()).orElseThrow());
            bom.setQty(c.getQty());
            bomRepository.save(bom);
        }
    }
    @Transactional
    public void deleteBomsByParentIds(List<Long> parentIds) {
        for (Long parentId : parentIds) {
            bomRepository.deleteByParentItemId(parentId);
        }
    }
/*
    public Page<BomProductItemDTO> getPagedParentProductList(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bomRepository.findAllParentItems(pageable);
        }
        return bomRepository.findPagedByKeyword(keyword, pageable);
    }
    */
    public Page<BomProductItemDTO> getPagedParentProductList(String keyword, Pageable pageable) {
        return bomRepository.findPagedParentItemsByKeyword(keyword, pageable);
    }


}
