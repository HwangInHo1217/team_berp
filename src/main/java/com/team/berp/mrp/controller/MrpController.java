package com.team.berp.mrp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MrpController {
    @GetMapping("/mrp/mrp")
    public String mrpPage() {
        return "mrp/mrp";
    }
    
    
}
