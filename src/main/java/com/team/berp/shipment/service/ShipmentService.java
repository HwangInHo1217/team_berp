package com.team.berp.shipment.service;

import com.team.berp.shipment.dto.*;
import java.util.List;

/**
 * 출고(Shipment) 모듈 Service 인터페이스
 */
public interface ShipmentService {

  /** 전체 출고 내역 조회 */
  List<ShipmentDetailDto> getAllShipments();

  /** 출고 대기(Pre-ship) 주문 목록 조회 */
  List<OrderSummaryDto> getPendingOrders();

  /** 특정 주문의 상세 정보 조회 (출고 등록 모달) */
  OrderDetailDto getOrderDetail(Long orderId);

  /** 특정 출고 내역 상세 조회 (출고 상세 모달) */
  ShipmentDetailDto getShipmentDetail(Long shipmentId);

  /** 선택된 주문들을 '가출고(N)' 상태로 로그 생성 */
  void preShipOrders(List<Long> orderIds);

  /** 선택된 출고 로그들을 '출고(Y)' 상태로 변경 & 재고 차감 */
  void shipOrders(List<Long> orderIds);

  /** 출고 내역 수정 (N→Y 전환 시 재고 차감 등) */
  void updateShipment(Long shipmentId, ShipmentDetailDto form);
}