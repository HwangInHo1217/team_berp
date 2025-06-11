package com.team.berp.place.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.team.berp.client.repository.ClientRepository;
import com.team.berp.domain.Company;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.Employee;
import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.employee.repository.EmployeeRepository;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import com.team.berp.place.dto.PlaceDTO;
import com.team.berp.place.dto.PlaceDTO.OrderLineItemDTO;
import com.team.berp.place.repository.PlaceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service //서비스 계층 담당
@RequiredArgsConstructor //final로 선언된 필드들에 대해 생성자 자동 주입
public class PlaceServiceImpl implements PlaceService{ //실제 구현 

	private final ClientRepository companyRepository;
    private final PlaceRepository companyOrderRepository;
    /*PlaceRepository가 CompanyOrderRepository를 상속받고 있기 때문에
     * ComPanyOrderRepository를 새로 생성 x, PlaceRepository를 사용하였음*/
    private final ItemRepository itemRepository;
    private final Order_OrderLineItemRepository orderLineItemRepository;
    private final EmployeeRepository employeeRepository;  // 생성자 주입으로 추가

	 
	
    @Override
    @Transactional //예외 발생시 자동으로 rollback
    public CompanyOrder registerOrder(PlaceDTO dto) {
        CompanyOrder order = new CompanyOrder(); //빈 발주 엔티티 객체 생성

//        // 1. Company 조회
        companyRepository.findById(dto.getCompanyId()) //dto.getCompanyId()로 전달받은 ID로 회사 엔티티 조회
            .ifPresentOrElse(order::setCompany, () -> { //조회시 데이터 존재하면 order.setCompany로 발주에 연결
                throw new RuntimeException("회사 정보가 없습니다."); //조회시 데이터가 없으면 예외 발생, 저장 막음
            });

        // 2. orderType 변환: String -> Enum
//        문자열로 넘어온 CUSTOMER, SUPPLIER를 enum 타입으로 변환, 잘못된 문자열이면 예외 발생
        CompanyOrder.OrderType orderType;
        try {
            orderType = CompanyOrder.OrderType.valueOf(dto.getOrderType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("잘못된 주문 유형입니다.");
        }
        order.setOrderType(orderType);

        //orderDate 변환: String -> LocalDateTime, 발주일 
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate orderDate = LocalDate.parse(dto.getOrderDate(), formatter);
        order.setOrderDate(orderDate);

         //비고 코멘트 세팅 (필드 추가되면 Entity에도 추가해야 함)
         order.setNote(dto.getNote()); // Entity에 comment 필드가 있으면

       
         //주문번호 생성 및 세팅
         String orderNum = generateOrderNum();
         order.setOrderNum(orderNum);
         
      // 2. 주문 상태 기본값 세팅
         order.setOrderStatus(CompanyOrder.OrderStatus.WAITING); // ✅ 이 줄 추가
         
        //발주 등록
        //발주 저장
        CompanyOrder savedOrder = companyOrderRepository.save(order);

        // 6. 품목 리스트 저장
        List<OrderLineItemDTO> lineItems = dto.getLineItems();
        long totalOrderQty = 0L;
        long totalAmount = 0L;

        if (lineItems != null && !lineItems.isEmpty()) {
            for (OrderLineItemDTO itemDTO : lineItems) {
                OrderLineItem lineItem = new OrderLineItem();

                // 품목 엔티티 조회
                Item item = itemRepository.findById(itemDTO.getItemId())
                    .orElseThrow(() -> new RuntimeException("해당 품목이 존재하지 않습니다."));

                // 발주와 품목 연결
                lineItem.setCompanyOrder(savedOrder);
                lineItem.setItem(item); 

                // 수량 세팅
                lineItem.setUnitQty(itemDTO.getUnitQty().intValue()); // Long → Integer

                // 단가 세팅 (필요하면)
                lineItem.setUnitPrice(itemDTO.getUnitPrice());

                // unit 필드 반드시 세팅
                lineItem.setUnit(itemDTO.getUnit());
                
                // 추가 계산 및 저장
                Long unitPriceAll = itemDTO.getUnitQty() * itemDTO.getUnitPrice();
                lineItem.setUnitPriceall(unitPriceAll); // DB에 필드 있다면

                // DB 저장
                orderLineItemRepository.save(lineItem);

                // 총합 계산
                totalOrderQty += itemDTO.getUnitQty();
                totalAmount += unitPriceAll;
            }
        }
       // 총합을 발주에 반영
        savedOrder.setOrderQty((int) totalOrderQty); // long → int
        savedOrder.setAmount(totalAmount);
        companyOrderRepository.save(savedOrder); // 다시 저장

        return savedOrder;
    }
    
    @Override
    public List<CompanyOrder> getAllOrders() {
//    	return companyOrderRepository.findAll();
    	return companyOrderRepository.findAllWithItems();
    }
    

    @Override
    public List<Item> findByType(ItemType type) {
    	return itemRepository.findByType(type);
    }
    
    
    private String generateOrderNum() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4);
        return "PO-" + datePart + "-" + randomPart;
    }
    
    @Override
    public Optional<Employee> getEmployeeByCompanyId(Long companyId) {
        return companyRepository.findById(companyId)
            .map(Company::getEmployee); // 회사가 존재하면 연결된 직원 Optional 반환
    }

    
    @Override
    public PlaceDTO getPlaceEditData(Long lineItemId) {
    	// orderLineItemRepository 등에서 데이터 조회
        OrderLineItem lineItem = orderLineItemRepository.findById(lineItemId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 품목입니다."));

        CompanyOrder order = lineItem.getCompanyOrder();// order 변수 선언
        Company company = order.getCompany();
        Employee employee = company.getEmployee();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<OrderLineItemDTO> lineItemDTOs = order.getLineItems().stream()
        	    .map(item -> {
        	        Item entityItem = item.getItem();
        	        Long unitQty = item.getUnitQty() != null ? item.getUnitQty().longValue() : 0L;
        	        Long unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : 0L;
        	        Long unitPriceAll = unitQty * unitPrice;

        	        return OrderLineItemDTO.builder()
        	            .orderLineItemId(item.getOrderLineItemId())
        	            .itemId(entityItem.getId())
        	            .itemName(entityItem.getName())
        	            .itemCode(entityItem.getCode())
        	            .unit(item.getUnit() != null ? item.getUnit() : "")
        	            .unitPrice(unitPrice)
        	            .unitQty(unitQty)
        	            .unitPriceAll(unitPriceAll)
        	            .build();
        	    })
        	    .collect(Collectors.toList());

        	// 총 주문 수량 (orderQty) = 개별 unitQty 합계
        	int totalOrderQty = lineItemDTOs.stream()
        	    .mapToInt(dto -> dto.getUnitQty() != null ? dto.getUnitQty().intValue() : 0)
        	    .sum();

        	// 총 금액 (amount) = unitPriceAll 합계
        	long totalAmount = lineItemDTOs.stream()
        	    .mapToLong(dto -> dto.getUnitPriceAll() != null ? dto.getUnitPriceAll() : 0L)
        	    .sum();

        	return PlaceDTO.builder()
        	    .orderId(order.getOrderId())
        	    .orderNum(order.getOrderNum())
        	    .orderDate(order.getOrderDate().format(formatter))
        	    .orderType(order.getOrderType().name())
        	    .companyId(company.getCompanyId())
        	    .note(order.getNote())
        	    .employeeId(employee.getEmployeeId())
        	    .employeeName(employee.getEmpName())
        	    .employeeTel(employee.getEmpTel())
        	    .employeeEmail(employee.getEmpEmail())
        	    .lineItems(lineItemDTOs)
        	    .orderQty(totalOrderQty)  // 전체 총 주문 수량
        	    .amount(totalAmount)     // 전체 총 금액
        	    .build();

    }

    @Override
    @Transactional
    public CompanyOrder updateOrder(PlaceDTO dto) {
        try {
            // 1. 기존 발주 (CompanyOrder) 조회
            CompanyOrder order = companyOrderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 발주입니다."));

            if (!canEdit(order.getOrderStatus())) {
                throw new RuntimeException("현재 상태에서는 수정할 수 없습니다. 상태: " + order.getOrderStatus());
            }
            
            System.out.println("✅ 수정할 발주 조회 성공: " + order.getOrderId());

            // 2. 회사 정보 변경
            if (dto.getCompanyId() != null) {
                companyRepository.findById(dto.getCompanyId())
                    .ifPresentOrElse(order::setCompany, () -> {
                        throw new RuntimeException("회사 정보가 없습니다.");
                    });
            }

            // 3. 주문 타입 변경
            try {
                CompanyOrder.OrderType orderType = CompanyOrder.OrderType.valueOf(dto.getOrderType());
                order.setOrderType(orderType);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("잘못된 주문 유형입니다: " + dto.getOrderType());
            }

            // 4. 주문 날짜 변경
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate orderDate = LocalDate.parse(dto.getOrderDate(), formatter);
            order.setOrderDate(orderDate);

            // 5. 비고 변경
            order.setNote(dto.getNote());

            // 6. 기존 발주 품목들을 개별적으로 업데이트 (삭제 후 재등록 방식 개선)
            List<OrderLineItem> existingItems = order.getLineItems();
            System.out.println("✅ 기존 품목 개수: " + existingItems.size());
            
            // 기존 품목들을 Map으로 변환 (orderLineItemId를 키로 사용)
            Map<Long, OrderLineItem> existingItemMap = existingItems.stream()
                .collect(Collectors.toMap(OrderLineItem::getOrderLineItemId, item -> item));

            long totalOrderQty = 0L;
            long totalAmount = 0L;

            // 7. DTO에서 온 품목들을 처리
            List<OrderLineItemDTO> lineItems = dto.getLineItems();
            if (lineItems != null && !lineItems.isEmpty()) {
                for (OrderLineItemDTO itemDTO : lineItems) {
                    OrderLineItem lineItem;
                    
                    // orderLineItemId가 있으면 기존 항목 수정, 없으면 새로 생성
                    if (itemDTO.getOrderLineItemId() != null && 
                        existingItemMap.containsKey(itemDTO.getOrderLineItemId())) {
                        
                        // 기존 항목 수정
                        lineItem = existingItemMap.get(itemDTO.getOrderLineItemId());
                        System.out.println("✅ 기존 품목 수정: " + lineItem.getOrderLineItemId());
                        
                        // Map에서 제거 (나중에 삭제되지 않도록)
                        existingItemMap.remove(itemDTO.getOrderLineItemId());
                    } else {
                        // 새 항목 생성
                        lineItem = new OrderLineItem();
                        lineItem.setCompanyOrder(order);
                        System.out.println("✅ 새 품목 생성");
                    }

                    // 품목 엔티티 조회
                    Item item = itemRepository.findById(itemDTO.getItemId())
                        .orElseThrow(() -> new RuntimeException("해당 품목이 존재하지 않습니다: " + itemDTO.getItemId()));

                    // 데이터 설정
                    lineItem.setItem(item);
                    lineItem.setUnitQty(itemDTO.getUnitQty().intValue());
                    lineItem.setUnitPrice(itemDTO.getUnitPrice());
                    lineItem.setUnit(itemDTO.getUnit());

                    Long unitPriceAll = itemDTO.getUnitQty() * itemDTO.getUnitPrice();
                    lineItem.setUnitPriceall(unitPriceAll);

                    // 저장
                    orderLineItemRepository.save(lineItem);

                    // 총합 계산
                    totalOrderQty += itemDTO.getUnitQty();
                    totalAmount += unitPriceAll;
                }
            }

            // 8. Map에 남아있는 항목들은 삭제 (DTO에서 제거된 항목들)
            if (!existingItemMap.isEmpty()) {
                System.out.println("✅ 삭제할 품목 개수: " + existingItemMap.size());
                orderLineItemRepository.deleteAll(existingItemMap.values());
            }

            // 9. 총 수량, 총 금액 업데이트
            order.setOrderQty((int) totalOrderQty);
            order.setAmount(totalAmount);

            // 10. 발주 저장
            CompanyOrder updatedOrder = companyOrderRepository.save(order);
            System.out.println("✅ 발주 수정 완료: " + updatedOrder.getOrderId());

            return updatedOrder;
            
        } catch (Exception e) {
            System.err.println("❌ 발주 수정 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("발주 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public PlaceDTO getPlaceDetailData(Long lineItemId) {
        try {
            OrderLineItem lineItem = orderLineItemRepository.findById(lineItemId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 품목입니다."));

            CompanyOrder order = lineItem.getCompanyOrder();
            Company company = order.getCompany();
            Employee employee = company.getEmployee();

            // ✅ 올바른 디버깅 코드
            System.out.println("========== 상세 조회 디버깅 ==========");
            System.out.println("lineItemId: " + lineItemId);
            System.out.println("orderId: " + order.getOrderId());
            System.out.println("companyId: " + company.getCompanyId());
            System.out.println("companyName: '" + company.getCompanyName() + "'");
            System.out.println("companyName null 체크: " + (company.getCompanyName() == null));
            System.out.println("companyName 빈문자열 체크: " + (company.getCompanyName() != null && company.getCompanyName().trim().isEmpty()));
            System.out.println("=====================================");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // 해당 발주의 모든 품목 정보
            List<OrderLineItemDTO> lineItemDTOs = order.getLineItems().stream()
                .map(item -> {
                    Item entityItem = item.getItem();
                    Long unitQty = item.getUnitQty() != null ? item.getUnitQty().longValue() : 0L;
                    Long unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : 0L;
                    Long unitPriceAll = unitQty * unitPrice;

                    // ✅ itemType 변환 로직
                    String itemTypeString;
                    if (entityItem.getType() == ItemType.raw) {
                        itemTypeString = "자재";
                    } else if (entityItem.getType() == ItemType.product) {
                        itemTypeString = "완제품";
                    } else {
                        itemTypeString = entityItem.getType().name();
                    }

                    return OrderLineItemDTO.builder()
                        .orderLineItemId(item.getOrderLineItemId())
                        .itemId(entityItem.getId())
                        .itemName(entityItem.getName())
                        .itemCode(entityItem.getCode())
                        .itemType(itemTypeString)
                        .unit(item.getUnit() != null ? item.getUnit() : "")
                        .unitPrice(unitPrice)
                        .unitQty(unitQty)
                        .unitPriceAll(unitPriceAll)
                        .build();
                })
                .collect(Collectors.toList());

            // 총 주문 수량과 금액 계산
            int totalOrderQty = lineItemDTOs.stream()
                .mapToInt(dto -> dto.getUnitQty() != null ? dto.getUnitQty().intValue() : 0)
                .sum();

            long totalAmount = lineItemDTOs.stream()
                .mapToLong(dto -> dto.getUnitPriceAll() != null ? dto.getUnitPriceAll() : 0L)
                .sum();

            // ✅ companyName null/빈값 처리
            String companyName = company.getCompanyName();
            if (companyName == null || companyName.trim().isEmpty()) {
                companyName = "회사명 미등록 (ID: " + company.getCompanyId() + ")";
                System.out.println("⚠️ 회사명이 null이거나 빈값입니다. companyId: " + company.getCompanyId());
            }

            return PlaceDTO.builder()
                .orderId(order.getOrderId())
                .orderNum(order.getOrderNum())
                .orderDate(order.getOrderDate().format(formatter))
                .orderType(order.getOrderType().name())
                .orderStatus(order.getOrderStatus().name())
                .companyId(company.getCompanyId())
                .companyName(companyName) // ✅ 처리된 회사명 사용
                .note(order.getNote())
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getEmpName())
                .employeeTel(employee.getEmpTel())
                .employeeEmail(employee.getEmpEmail())
                .lineItems(lineItemDTOs)
                .orderQty(totalOrderQty)
                .amount(totalAmount)
                .build();
                
        } catch (Exception e) {
            System.err.println("❌ 상세 조회 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public List<CompanyOrder> searchOrdersByItemName(String itemName) {
        try {
            System.out.println("✅ 품목명 검색 시작 - 키워드: " + itemName);
            
            // 방법 1: 수정된 @Query 메서드 사용
            List<CompanyOrder> searchResults = companyOrderRepository.findOrdersByItemName(itemName);
            
            // 방법 2: 메서드명 기반 사용 (위에서 구현한 경우)
            // List<CompanyOrder> searchResults = companyOrderRepository.findByLineItemsItemNameContainingIgnoreCase(itemName);
            
            // 방법 3: Native Query 사용 (위에서 구현한 경우)
            // List<CompanyOrder> searchResults = companyOrderRepository.findOrdersByItemNameNative(itemName);
            
            System.out.println("✅ 검색 완료 - 결과 개수: " + searchResults.size());
            return searchResults;
            
        } catch (Exception e) {
            System.err.println("❌ 검색 오류: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
//    @Override
//    @Transactional
//    public CompanyOrder updateOrderStatus(Long orderId, String nextStatus) {
//        CompanyOrder order = companyOrderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("해당 발주가 존재하지 않습니다."));
//
//        CompanyOrder.OrderStatus currentStatus = order.getOrderStatus();
//        CompanyOrder.OrderStatus newStatus;
//
//        try {
//            newStatus = CompanyOrder.OrderStatus.valueOf(nextStatus);
//        } catch (IllegalArgumentException e) {
//            throw new RuntimeException("잘못된 상태값입니다: " + nextStatus);
//        }
//
//        // 상태 전이 허용 조건 확인
//        if ((currentStatus == CompanyOrder.OrderStatus.WAITING && newStatus == CompanyOrder.OrderStatus.CONFIRMED) ||
//            (currentStatus == CompanyOrder.OrderStatus.CONFIRMED && newStatus == CompanyOrder.OrderStatus.COMPLETED)) {
//            order.setOrderStatus(newStatus);
//        } else {
//            throw new RuntimeException("허용되지 않은 상태 전이입니다: " + currentStatus + " → " + newStatus);
//        }
//
//        return companyOrderRepository.save(order);
//    }
    
    @Transactional
    @Override
    public CompanyOrder updateOrderStatusEnum(Long orderId, CompanyOrder.OrderStatus status) {
        CompanyOrder order = companyOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("해당 발주를 찾을 수 없습니다. orderId: " + orderId));

        order.setOrderStatus(status);

        return companyOrderRepository.save(order);
    }

    
    /* 상태 변경 메서드 (예: 발주 대기 → 발주 등록) */
    @Transactional
    @Override
    public boolean changeOrderStatus(Long orderId, String newStatus) {
        Optional<CompanyOrder> optionalOrder = companyOrderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            return false;
        }

        CompanyOrder order = optionalOrder.get();

        try {
            // 🔽 문자열을 enum으로 변환
            CompanyOrder.OrderStatus statusEnum = CompanyOrder.OrderStatus.valueOf(newStatus.toUpperCase());

            // 🔽 enum으로 상태 설정
            order.setOrderStatus(statusEnum);

            // 🔽 저장
            companyOrderRepository.save(order);
            return true;

        } catch (IllegalArgumentException e) {
            // 잘못된 상태값이 들어오면 예외 발생 → 실패 처리
            System.err.println("❌ 상태값 변환 실패: " + newStatus);
            return false;
        }
    }

 // 상태별 액션 가능 여부 체크
    @Override
    public boolean canEdit(CompanyOrder.OrderStatus status) {
        return status == CompanyOrder.OrderStatus.WAITING;
    }

    @Override
    public boolean canConfirm(CompanyOrder.OrderStatus status) {
        return status == CompanyOrder.OrderStatus.WAITING;
    }

    @Override
    public boolean canReceive(CompanyOrder.OrderStatus status) {
        return status == CompanyOrder.OrderStatus.CONFIRMED;
    }

    // 발주 확정 (WAITING → CONFIRMED)
    @Override
    @Transactional
    public CompanyOrder confirmOrder(Long orderId) {
        CompanyOrder order = companyOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("해당 발주를 찾을 수 없습니다."));
            
        if (!canConfirm(order.getOrderStatus())) {
            throw new RuntimeException("발주 확정할 수 없는 상태입니다. 현재 상태: " + order.getOrderStatus());
        }
        
        order.setOrderStatus(CompanyOrder.OrderStatus.CONFIRMED);
        return companyOrderRepository.save(order);
    }

    // 입고 완료 (CONFIRMED → COMPLETED)
    @Override
    @Transactional
    public CompanyOrder completeOrder(Long orderId) {
        CompanyOrder order = companyOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("해당 발주를 찾을 수 없습니다."));
            
        if (!canReceive(order.getOrderStatus())) {
            throw new RuntimeException("입고 처리할 수 없는 상태입니다. 현재 상태: " + order.getOrderStatus());
        }
        
        order.setOrderStatus(CompanyOrder.OrderStatus.COMPLETED);
        return companyOrderRepository.save(order);
    }
    
 // PlaceServiceImpl.java에 추가할 메서드

    @Override
    @Transactional
    public String deleteOrderLineItem(Long lineItemId) {
        try {
            System.out.println("✅ 스마트 삭제 시작 - lineItemId: " + lineItemId);
            
            // 1. 삭제할 품목 조회
            OrderLineItem lineItem = orderLineItemRepository.findById(lineItemId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 품목입니다. ID: " + lineItemId));
            
            CompanyOrder order = lineItem.getCompanyOrder();
            System.out.println("✅ 발주서 조회 완료 - orderId: " + order.getOrderId());
            
            // 2. 삭제 가능 상태인지 체크
            if (!canEdit(order.getOrderStatus())) {
                throw new RuntimeException("현재 상태에서는 삭제할 수 없습니다. 상태: " + order.getOrderStatus());
            }
            
            // 3. 해당 발주의 전체 품목 개수 확인
            List<OrderLineItem> allItems = order.getLineItems();
            System.out.println("✅ 전체 품목 개수: " + allItems.size());
            
            if (allItems.size() <= 1) {
                // 📌 마지막 품목인 경우 발주 전체 삭제
                System.out.println("✅ 마지막 품목 삭제 - 발주서 전체 삭제 실행");
                
                String orderNum = order.getOrderNum();
                companyOrderRepository.delete(order);
                
                System.out.println("✅ 발주서 삭제 완료 - orderNum: " + orderNum);
                return String.format("발주서 %s가 완전히 삭제되었습니다.", orderNum);
                
            } else {
                // 📌 품목만 삭제 (발주서는 유지)
                System.out.println("✅ 개별 품목 삭제 실행");
                
                String itemName = lineItem.getItem().getName();
                
                // 품목 삭제
                orderLineItemRepository.delete(lineItem);
                System.out.println("✅ 품목 삭제 완료 - itemName: " + itemName);
                
                // 4. 발주의 총 수량과 금액 재계산
                order.getLineItems().remove(lineItem); // 컬렉션에서도 제거
                
                int totalQty = 0;
                long totalAmount = 0;
                
                for (OrderLineItem remainingItem : order.getLineItems()) {
                    int qty = remainingItem.getUnitQty() != null ? remainingItem.getUnitQty() : 0;
                    long price = remainingItem.getUnitPrice() != null ? remainingItem.getUnitPrice() : 0L;
                    
                    totalQty += qty;
                    totalAmount += (qty * price);
                }
                
                // 5. 발주서 업데이트
                order.setOrderQty(totalQty);
                order.setAmount(totalAmount);
                companyOrderRepository.save(order);
                
                System.out.println("✅ 발주서 업데이트 완료 - 총 수량: " + totalQty + ", 총 금액: " + totalAmount);
                
                return String.format("품목 '%s'이(가) 삭제되었습니다. (남은 품목: %d개)", 
                                   itemName, order.getLineItems().size());
            }
            
        } catch (RuntimeException e) {
            System.err.println("❌ 스마트 삭제 중 비즈니스 오류: " + e.getMessage());
            throw e; // 비즈니스 예외는 그대로 전달
            
        } catch (Exception e) {
            System.err.println("❌ 스마트 삭제 중 시스템 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("삭제 처리 중 시스템 오류가 발생했습니다: " + e.getMessage());
        }
    }

}