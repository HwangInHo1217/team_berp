//package com.team.berp.place.controller;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//
//import com.team.berp.domain.Item;
//import com.team.berp.domain.ItemType;
//import com.team.berp.item.controller.ItemController;
//import com.team.berp.item.repository.ItemRepository;
//
//@RestController
//@RequestMapping("/api/items")
//public class PlaceApiController {
//
//	private final ItemRepository itemrepository;
//
//	public ItemController(ItemRepository itemrepository) {
//        this.itemrepository = itemrepository;
//    }
//
//    // 1) 품목 타입 전체 리스트 반환 (Enum 이름들)
//    @GetMapping("/types")
//    public List<String> getItemTypes() {
//        return Arrays.stream(ItemType.values())
//                .map(Enum::name)
//                .collect(Collectors.toList());
//    }
//
//    // 2) 특정 품목 타입에 해당하는 품목 리스트 반환 (id, name)
//    @GetMapping("/names")
//    public List<ItemNameDto> getItemsByType(@RequestParam String type) {
//        ItemType itemType = ItemType.valueOf(type);
//        List<Item> items = itemrepository.findByType(itemType);
//        return items.stream()
//                .map(item -> new ItemNameDto(item.getId(), item.getName()))
//                .collect(Collectors.toList());
//    }
//
//    // 3) 품목 id로 단위, 단가 정보 반환
//    @GetMapping("/{id}/details")
//    public ItemDetailDto getItemDetails(@PathVariable  Long id) {
//        Item item = ItemRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
//        return new ItemDetailDto(item.getUnit(), item.getUnitPrice());
//    }
//
//    // DTO 클래스들 (내부 클래스 또는 별도 파일)
//    public static record ItemNameDto(Long id, String name) {}
//    public static record ItemDetailDto(String unit, Long unitPrice) {}
//}
//
