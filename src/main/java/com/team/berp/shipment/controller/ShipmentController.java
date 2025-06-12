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

@RequiredArgsConstructor
public class ShipmentController {

    

    /** 출고 메인 화면 */
    @GetMapping("/shipment")
    public String viewShipments(Model model) {
      
        return "shipment/shipment";
    }

}