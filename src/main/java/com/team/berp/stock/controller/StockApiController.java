package com.team.berp.stock.controller;

import com.team.berp.stock.dto.StockRequestDTO;
import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockApiController {

    private final StockService stockSvc;

    @GetMapping
    public ResponseEntity<Page<StockResponseDTO>> getStocks(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "whs", required = false) String whs,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<StockResponseDTO> result = stockSvc.getList(keyword, whs, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{stockId}/detail")
    public ResponseEntity<StockResponseDTO> getDetail(@PathVariable Long stockId) {
        StockResponseDTO result = stockSvc.getDetail(stockId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/in")
    public ResponseEntity<String> stockIn(@RequestBody StockRequestDTO req) {
        stockSvc.stockIn(req);
        return ResponseEntity.ok("입고 처리 완료");
    }

    @PostMapping("/out")
    public ResponseEntity<String> stockOut(@RequestBody StockRequestDTO req) {
        stockSvc.stockOut(req);
        return ResponseEntity.ok("출고 처리 완료");
    }

    @PostMapping("/dispose")
    public ResponseEntity<String> dispose(@RequestBody StockRequestDTO req) {
        stockSvc.dispose(req);
        return ResponseEntity.ok("폐기 처리 완료");
    }

    @PostMapping("/return-in")
    public ResponseEntity<String> returnIn(@RequestBody StockRequestDTO req) {
        stockSvc.returnIn(req);
        return ResponseEntity.ok("반품입고 처리 완료");
    }
}