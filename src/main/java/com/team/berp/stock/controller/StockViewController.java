package com.team.berp.stock.controller;

import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StockViewController {

    private final StockService stockSvc;

    @GetMapping("/stock")
    public String stock(Model model) {
        Pageable page = PageRequest.of(0, 10, Sort.by("id").descending());
        Page<StockResponseDTO> stocks = stockSvc.getList(null, null, page);
        model.addAttribute("stocks", stocks);
        return "stock/stock";
    }
}