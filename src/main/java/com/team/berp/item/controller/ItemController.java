package com.team.berp.item.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.team.berp.domain.Item;
import com.team.berp.item.dto.ItemListViewResponse;
import com.team.berp.item.service.ItemService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class ItemController {
	
	private final ItemService itemService;
	
	@GetMapping("/item/item") // 클라이언트가 "/item/item" URL로 GET 요청을 보낼 때 실행됨
	public String item(
	    @RequestParam(name="type", value = "type", required = false) String type,       // 검색 조건 종류 (ex: itemName, itemCode)
	    @RequestParam(name="keyword", value = "keyword", required = false) String keyword, // 검색 키워드 (사용자가 입력한 검색어)
	    @RequestParam(name="page", defaultValue = "0") int page,                        // 현재 페이지 번호 (0부터 시작), 기본값 0
	    @RequestParam(name="size", defaultValue = "10") int size,                       // 한 페이지에 보여줄 아이템 수, 기본값 10
	    @RequestParam(name = "tab", required = false, defaultValue = "all") String tab, // 탭으로 구분해야함
	    Model model                                                        // 뷰에 데이터를 전달할 Spring의 모델 객체
	) {
	    // 페이지 정보와 정렬 기준(itemId 내림차순)을 담은 Pageable 객체 생성
	    Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

	    // 페이지네이션 결과를 담을 변수 선언
	    Page<Item> itemPage;

	    // 검색 조건(type과 keyword)이 모두 유효하면 → 검색 + 페이징 조회
	    if (type != null && keyword != null && !keyword.isBlank()) {
	        itemPage = itemService.searchItemsWithPaging(type, keyword, pageable);
	    } else {
	        // 검색 조건이 없으면 전체 목록을 페이징으로 조회
	        itemPage = itemService.getItemList(pageable);
	    }

	    // View(템플릿)로 전달할 모델에 결과 추가
	    model.addAttribute("itemPage", itemPage);   // 페이징된 아이템 목록 전달
	    model.addAttribute("type", type);           // 선택된 검색 타입 유지
	    model.addAttribute("keyword", keyword);     // 입력된 검색어 유지

	    // 반환할 뷰의 이름 (템플릿: src/main/resources/templates/item/item.html)
	    return "item/item";
	}




}
