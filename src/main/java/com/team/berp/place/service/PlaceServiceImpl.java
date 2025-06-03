package com.team.berp.place.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                lineItem.setUnitQty(itemDTO.getUnitQty());

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
        savedOrder.setOrderQty(totalOrderQty);
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
        return companyRepository.findById(companyId) // 1) companyRepository.findById(...)로 Company 엔티티 가져옴
                .map(Company::getEmployee); // 2) Company.getEmployee()가 반환하는 Optional<Employee>
    }

 
}
