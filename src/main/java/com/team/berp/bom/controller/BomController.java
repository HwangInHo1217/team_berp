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
	/*@GetMapping("/bom/bom")
	public String showBomPage(
	        @RequestParam(name = "type", required = false) String type,
	        @RequestParam(name = "keyword", required = false) String keyword,
	        @RequestParam(name = "useYn", required = false) String useYn,
	        @PageableDefault(size = 10) Pageable pageable,
	        Model model) {

	    // ✅ type, keyword, useYn을 모두 넘김
	    Page<BomProductItemDTO> bomPage = bomService.getPagedParentProductList(type, keyword, useYn, pageable);
	    System.out.println("bom_page_getContent: " + bomPage.getContent());

	    model.addAttribute("product", bomPage.getContent());
	    model.addAttribute("bomPage", bomPage);
	    model.addAttribute("type", type);
	    model.addAttribute("keyword", keyword);
	    model.addAttribute("useYn", useYn); // ✅ 선택 값 유지용

	    List<BomProductItemDTO> productList = bomService.getParentProductDTOList();
	    model.addAttribute("productList", productList);

	    ItemSelectionDTO itemSelectionDto = bomService.getSelectableItems();
	    model.addAttribute("selectMaterialList", itemSelectionDto.getMaterials());
	    model.addAttribute("selectProductList", itemSelectionDto.getProducts());

	    return "bom/bom";
	}*/
	  @GetMapping("/bom")
	    public String bomViewPage() {
	        return "bom/bom";  // => resources/templates/bom/bom.html
	    }



	
}
