//package com.team.berp.place.service;
//
//import org.springframework.stereotype.Service;
//
//import com.team.berp.client.repository.ClientRepository;
//import com.team.berp.domain.CompanyOrder;
//import com.team.berp.item.repository.ItemRepository;
//import com.team.berp.place.dto.PlaceDTO;
//import com.team.berp.place.repository.PlaceRepository;
//
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//
//@Service //서비스 계층 담당
//@RequiredArgsConstructor //final로 선언된 필드들에 대해 생성자 자동 주입
//public class PlaceServiceImpl implements PlaceService{ //실제 구현 
//
//	private final ClientRepository companyRepository;
//    private final PlaceRepository CompanyOrderRepository;
//    /*PlaceRepository가 CompanyOrderRepository를 상속받고 있기 때문에
//     * ComPanyOrderRepository를 새로 생성 x, PlaceRepository를 사용하였음*/
//    private final ItemRepository itemRepository;
//    //private final OrderLineItemRepository orderLineItemRepository;
//	
//	
//	@Override
//	@Transactional
//		public CompanyOrder registerOrder(PlaceDTO dto) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//}
