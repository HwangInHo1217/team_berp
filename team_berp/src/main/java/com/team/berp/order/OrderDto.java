package com.team.berp.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data								// DTO getter, setter
// @Entity(name = "order_line_item")		// 테이블 이름
// @NoArgsConstructor					// 어노테이션 파라미터가 없는 default 생성자를 자동으로 생성하는 형태
// @AllArgsConstructor					// 클래스의 모든 필드값을 파라미터로 받는 생성자를 자동으로 생성
public class OrderDto {
//	@Id		// 해당 컬럼을 primary key 지정
//	@Column(nullable = false, columnDefinition = "int auto_increment")
//	int order_line_item_id;
	int order_line_item_id;
	String order_id, item_id, quantity, unit_price;
}
