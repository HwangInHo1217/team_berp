package com.team.berp.bom.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.team.berp.bom.dto.BomProductItemDTO;
import com.team.berp.bom.dto.ItemSelectionDTO;
import com.team.berp.bom.service.BomService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor

public class BomController {
	
	private final BomService bomService;
	@GetMapping("/bom/bom")
	public String showBomPage(
	        @RequestParam(name="keyword", value = "keyword", required = false) String keyword,
	        @PageableDefault(size = 10) Pageable pageable,
	        Model model) {

	    // 검색 및 페이징 처리된 제품 리스트 (완제품)
	    Page<BomProductItemDTO> bomPage = bomService.getPagedParentProductList(keyword, pageable);
	    System.out.println("bom_page_getContent: "+bomPage.getContent());
	    System.out.println("bom_page: "+bomPage);
	    // 검색조건과 결과를 model에 추가
	    model.addAttribute("product", bomPage.getContent());
	    model.addAttribute("bomPage", bomPage);
	    model.addAttribute("keyword", keyword);
	    
	    
	    List<BomProductItemDTO> productList = bomService.getParentProductDTOList();
	    model.addAttribute("productList", productList);
	    // 자재/완제품 선택용 리스트 추가
	    ItemSelectionDTO itemSelectionDto = bomService.getSelectableItems();
	    model.addAttribute("selectMaterialList", itemSelectionDto.getMaterials());
	    model.addAttribute("selectProductList", itemSelectionDto.getProducts());

	    return "bom/bom";
	}

	
}
