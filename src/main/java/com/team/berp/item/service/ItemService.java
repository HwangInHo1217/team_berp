package com.team.berp.item.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.item.dto.AddItemRequestDTO;
import com.team.berp.item.dto.ItemListViewResponse;
import com.team.berp.item.dto.UpdateItemRequestDTO;
import com.team.berp.item.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

@Service // 서비스 레이어
@RequiredArgsConstructor
public class ItemService {

	private final ItemRepository itemRepository;
	
	public Item getItemById(Long id) {
	    return itemRepository.findById(id)
	        .orElseThrow(() -> new IllegalArgumentException("해당 ID의 품목을 찾을 수 없습니다: " + id));
	}
	public Page<Item> getItemList(Pageable pageable) {
		  // Repository에서 Pageable 기반으로 전체 아이템 목록을 페이징 조회
	    return itemRepository.findAll(pageable);
	}
	// 검색 조건과 페이징 정보를 받아서 Page<Item>으로 결과 반환
	public Page<Item> searchItemsWithPaging(String type, String keyword, Pageable pageable) {

	    // 검색 조건이 "itemName"인 경우 → itemName으로 LIKE 검색 + 페이징 처리
	    if ("itemName".equals(type) || "name".equals(type)) {
	        return itemRepository.findByNameContaining(keyword, pageable);
	    }

	    // 검색 조건이 "itemCode"인 경우 → itemCode로 LIKE 검색 + 페이징 처리
	    if ("itemCode".equals(type) || "code".equals(type)) {
	        return itemRepository.findByCodeContaining(keyword, pageable);
	    }

	    // 조건에 해당하지 않는 경우 빈 페이지 반환
	    return Page.empty();
	}

	
	// 검색 조건(type)과 키워드(keyword)에 따라 품목 목록을 필터링하고, DTO 리스트로 반환하는 메서드
	public List<ItemListViewResponse> searchItems(String type, String keyword) {
	    
	    // 1. 검색 결과를 담을 Item 리스트 선언
	    List<Item> results;

	    // 2. 검색 타입이 "name"이면 품목명 기준으로 부분 일치 검색 수행
	    if ("name".equals(type)) {
	        results = itemRepository.findByNameContaining(keyword);

	    // 3. 검색 타입이 "code"이면 품목코드 기준으로 부분 일치 검색 수행
	    } else if ("code".equals(type)) {
	        results = itemRepository.findByCodeContaining(keyword);

	    // 4. 잘못된 검색 타입이면 빈 리스트로 처리
	    } else {
	        results = Collections.emptyList();
	    }

	    // 5. 결과로 받은 Item 리스트를 DTO 리스트로 변환
	    List<ItemListViewResponse> dtoList = new ArrayList<>();

	    // 6. 각각의 Item 객체를 반복하면서 DTO로 변환 후 추가
	    for (Item item : results) {
	        ItemListViewResponse dto = new ItemListViewResponse(item);
	        dtoList.add(dto);
	    }

	    // 7. 최종적으로 변환된 DTO 리스트 반환
	    return dtoList;
	}
	@Transactional
	public void updateItem(Long id, UpdateItemRequestDTO dto) {
	    Item item = itemRepository.findById(id)
	            .orElseThrow(() -> new IllegalArgumentException("해당 품목이 존재하지 않습니다."));

	    item.update(dto); // → 아래의 도메인 메서드 참고
	}

	/*
	// 모든 품목(Item)을 조회한 후, View에 전달할 DTO(ItemListViewResponse) 리스트로 변환하여 반환하는 메서드
	public List<ItemListViewResponse> findAllItemsForView() {

	    // 1. item 테이블의 모든 데이터를 조회 (Entity 객체 리스트로 반환)
	    List<Item> items = itemRepository.findAll();

	    // 2. Entity -> DTO 변환을 위한 리스트 선언
	    List<ItemListViewResponse> dtoList = new ArrayList<>();

	    // 3. 조회된 모든 Item 엔티티를 순회하면서
	    for (Item item : items) {
	        // 4. 각 Item을 DTO로 변환하고 dtoList에 추가
	        dtoList.add(new ItemListViewResponse(item));
	    }

	    // 5. 최종적으로 DTO 리스트를 반환 (View에서 사용)
	    return dtoList;
	}*/
	

	public Long saveItem(AddItemRequestDTO dto) {
		// 품목 유형 추출
		ItemType type = ItemType.valueOf(dto.getType());

		// 품목 코드 자동 생성 (예: "RAW001", "PRODUCT002")
		String code = generateNextCode(type);
		
		// DTO를 Entity로 변환하여 저장
		Item item = dto.toEntity(code);
		System.out.println("단위 확인:"+dto.getUnit());
		System.out.println("단위 확인(객체):"+item.getUnit());
		return itemRepository.save(item).getId();
	}

	// 품목 유형(type)에 따라 필터링된 품목 리스트를 반환하는 메서드
	public List<Item> findItemsByType(String type) {
		// 전체 품목 목록을 데이터베이스에서 조회
		List<Item> allItems = itemRepository.findAll();
		// 필터링된 품목을 담을 리스트 생성
		List<Item> filtered = new ArrayList<>();
		// 전체 품목 목록을 하나씩 순회하면서
		for (Item item : allItems) {
			// 현재 품목의 유형이 요청된 type과 일치하는 경우
			if (type.equals(item.getType().toString())) {
				System.out.println(type+" "+item.getType()+", "+item.getType().getClass());
				System.out.println(type+" "+item.getType()+", "+item.getType().toString().getClass());
				// 결과 리스트에 해당 품목 추가
				filtered.add(item);
			}
		}
		// 필터링이 완료된 품목 리스트 반환
		return filtered;
	}

	// 품목 코드 자동 생성 메서드
	private String generateNextCode(ItemType type) {
	    String prefix = switch (type) {
	        case raw -> "RAW";
	        case product -> "PRD";
	    };

	    int nextNum = 1;

	    try {
	        List<String> codeList = itemRepository.findCodesByPrefix(prefix);
	        
	        if (!codeList.isEmpty()) {
	            String lastCode = codeList.get(0);
	            if (lastCode.length() >= 6) {
	                String numberPart = lastCode.substring(3); // "004"
	                nextNum = Integer.parseInt(numberPart) + 1;
	            }
	        }

	        return String.format("%s%03d", prefix, nextNum);
	    } catch (Exception e) {
	        System.out.println("이름 변환 중 예외: " + e.getMessage());
	        e.printStackTrace();
	        return String.format("%s%03d", prefix, nextNum); // fallback
	    }
	}
	public void deleteItems(List<Long> ids) {
	    itemRepository.deleteAllByIdInBatch(ids);
	}
	
	
	


}