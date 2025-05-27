package com.team.berp.order.apicontroller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.team.berp.order.dto.OrderDto;
import com.team.berp.order.service.OrderService;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderApiController {
    private final OrderService orderService;

    @GetMapping("/list")
    public ResponseEntity<List<OrderDto>> list() {
        return ResponseEntity.ok(orderService.findAllOrders());
    }

    @GetMapping("/unit/{itemId}")
    public ResponseEntity<Map<String,Object>> getUnit(@PathVariable Long itemId) {
        return ResponseEntity.ok(orderService.getUnitInfo(itemId));
    }
}