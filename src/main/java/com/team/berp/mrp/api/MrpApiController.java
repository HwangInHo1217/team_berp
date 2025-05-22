package com.team.berp.mrp.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.mrp.service.MrpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MrpApiController {

    private final MrpService mrpService;

    @GetMapping("/api/mrp")
    public List<MrpViewDto> getMrpList() {
        return mrpService.getMrpViewList(); // JSON으로 반환
    }
}