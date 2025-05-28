package com.team.berp.mrp.controller;

import com.team.berp.mrp.service.MrpService;
import com.team.berp.mrp.dto.MrpViewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MrpController {
    @GetMapping("/mrp/mrp")
    public String mrpPage() {
        return "/mrp/mrp";
    }
}
