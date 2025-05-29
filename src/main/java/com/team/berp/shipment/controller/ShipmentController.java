package com.team.berp.shipment.controller;

import com.team.berp.shipment.dto.*;
import com.team.berp.shipment.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Thymeleaf 기반 출고 화면 Controller
 */
@Controller
@RequestMapping("/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService service;

    /** 출고 메인 화면 */
    @GetMapping
    public String viewShipments(Model model) {
        model.addAttribute("shipments", service.getAllShipments());
        model.addAttribute("pendingOrders", service.getPendingOrders());
        return "shipment/shipment";
    }

    /** 가출고(N) 처리(모달에서 POST) */
    @PostMapping("/preShip")
    public String preShip(@RequestParam List<Long> orderIds) {
        service.preShipOrders(orderIds);
        return "redirect:/shipments";
    }

    /** 실제 출고(Y) 처리(모달에서 POST) */
    @PostMapping("/doShip")
    public String doShip(@RequestParam List<Long> shipmentIds) {
        service.shipOrders(shipmentIds);
        return "redirect:/shipments";
    }

    /** 출고 수정(모달에서 POST) */
    @PostMapping("/{id}/update")
    public String updateShipment(
        @PathVariable Long id,
        @ModelAttribute ShipmentDetailDto form
    ) {
        service.updateShipment(id, form);
        return "redirect:/shipments";
    }

    /** 주문 상세 조회(가출고/출고 등록 모달 AJAX fragment) */
    @GetMapping("/order/{orderId}/detail")
    public String orderDetail(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderDetail", service.getOrderDetail(orderId));
        return "shipment/shipment-register-detail-modal :: detail-fragment";
    }

    /** 출고 상세 조회(상세 모달 AJAX fragment) */
    @GetMapping("/{id}/detail")
    public String shipmentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("shipmentDetail", service.getShipmentDetail(id));
        return "shipment/shipment-detail-modal :: detail-fragment";
    }
}