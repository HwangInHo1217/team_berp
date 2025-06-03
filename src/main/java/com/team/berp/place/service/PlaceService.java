package com.team.berp.place.service;

import java.util.List;
import java.util.Optional;

import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.Employee;
import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.place.dto.PlaceDTO;

//비즈니스 로직 메서드 시그니처만 선언
//인터페이스만 보고도 어떤 기능이 있는 파악 가능하게 함
//나는 registerOrder 기능을 제공할 거야 라고 알려주는 설계서
public interface PlaceService {

//	private PlaceRepository placeRepository;
	
	//CompanyOrder: 메소드를 실행하면 CompanyOrder 객체 하나가 반환됨
	//registerOrder: 메소드 이름, 발주를 새로 등록하는 기능
	//PlaceDTO dto: 메소드가 입력으로 받는 인자 -> 사용자가 입력한 정보
	CompanyOrder registerOrder(PlaceDTO dto); //발주등록
	List<CompanyOrder> getAllOrders();
	List<Item> findByType(ItemType type);
	
	/* 회사 ID를 기준으로 Company 테이블을 조회하고,
     * 거기서 연결된 Employee(Optional)를 반환*/
	Optional<Employee> getEmployeeByCompanyId(Long companyId);

}
