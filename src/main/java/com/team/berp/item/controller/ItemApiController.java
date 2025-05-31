package com.team.berp.item.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.item.dto.AddItemRequestDTO;
import com.team.berp.item.dto.ItemListViewResponse;
import com.team.berp.item.dto.UpdateItemRequestDTO;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.item.service.ItemService;

import lombok.RequiredArgsConstructor;

@RestController // REST API용 컨트롤러 (View를 반환하지 않음)
@RequiredArgsConstructor // final 필드 기반 생성자 자동 생성 (DI 주입용)
public class ItemApiController {
	private final ItemRepository itemRepository;
    private final ItemService itemService; // 서비스 레이어 주입

    @PostMapping("item/item") // POST 방식 요청 처리
    public ResponseEntity<Long> saveItem(@RequestBody AddItemRequestDTO dto) {
        // 서비스에서 품목 저장 후 ID 반환
    	Long savedId= null;
    	try {
    		System.out.println("dto: " +dto.getName());
    		System.out.println("dto: " +dto.getType());
    		savedId = itemService.saveItem(dto);
    			
		} catch (Exception e) {
			System.out.println("saveItem 예외: "+e.getMessage());// TODO: handle exception
			e.printStackTrace();
		}
        return ResponseEntity.ok(savedId); // HTTP 200 OK + ID 반환
    }
    @DeleteMapping("item/delete")
    public ResponseEntity<?> deleteItems(@RequestBody List<Long> ids) {
        itemService.deleteItems(ids);
        return ResponseEntity.ok().build();
    }
    @PutMapping("item/{id}")
    public ResponseEntity<Void> updateItem(
            @PathVariable("id") Long id,
            @RequestBody UpdateItemRequestDTO dto) {

        itemService.updateItem(id, dto);
        return ResponseEntity.ok().build();
    }
 // ✅ 전체/자재/완제품 조회 + 검색 + 사용여부 + 페이징까지 처리
    @GetMapping("/api/item/list")
    public ResponseEntity<?> getItemList(
            @RequestParam(name = "tab", required = false, defaultValue = "all") String tab,
            @RequestParam(name = "type", required = false) String searchType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "useYn", required = false) String useYn,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Item> resultPage;

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasUseYn = useYn != null && !useYn.isBlank();
        boolean isFiltered = !"all".equalsIgnoreCase(tab);
        System.out.println("useYn"+useYn);
        // 🔍 enum 타입으로 변환
        ItemType itemType = null;
        if (isFiltered) {
            try {
                itemType = ItemType.valueOf(tab);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("잘못된 탭 파라미터");
            }
        }

        // 🔍 조건별 분기 처리
        if (hasKeyword && "name".equals(searchType)) {
            if (isFiltered && hasUseYn)
                resultPage = itemRepository.findByNameContainingAndTypeAndUse(keyword, itemType, useYn, pageable);
            else if (isFiltered)
                resultPage = itemRepository.findByNameContainingAndType(keyword, itemType, pageable);
            else
                resultPage = itemRepository.findByNameContaining(keyword, pageable);
        } else if (hasKeyword && "code".equals(searchType)) {
            if (isFiltered && hasUseYn)
                resultPage = itemRepository.findByCodeContainingAndTypeAndUse(keyword, itemType, useYn, pageable);
            else if (isFiltered)
                resultPage = itemRepository.findByCodeContainingAndType(keyword, itemType, pageable);
            else
                resultPage = itemRepository.findByCodeContaining(keyword, pageable);
        } else {
            if (isFiltered && hasUseYn) {
                resultPage = itemRepository.findByTypeAndUse(itemType, useYn, pageable);
            } else if (isFiltered) {
                resultPage = itemRepository.findByType(itemType, pageable);
            } else if (hasUseYn) {
                resultPage = itemRepository.findByUse(useYn, pageable);
            } else {
                resultPage = itemRepository.findAll(pageable);
            }
        }

        	
        Page<ItemListViewResponse> response = resultPage.map(ItemListViewResponse::new);
        return ResponseEntity.ok(response);
    }


}