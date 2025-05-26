package com.team.berp.item.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.item.dto.AddItemRequestDTO;
import com.team.berp.item.dto.UpdateItemRequestDTO;
import com.team.berp.item.service.ItemService;

import lombok.RequiredArgsConstructor;

@RestController // REST API용 컨트롤러 (View를 반환하지 않음)
@RequiredArgsConstructor // final 필드 기반 생성자 자동 생성 (DI 주입용)

public class ItemApiController {

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

}