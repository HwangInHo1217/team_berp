package com.team.berp.bom.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.bom.dto.AddBomRequestDTO;
import com.team.berp.bom.dto.BomListViewResponse;
import com.team.berp.bom.dto.UpdateBomRequestDTO;
import com.team.berp.bom.service.BomService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BomApiController {
	private final BomService bomService;

	@PostMapping("/bom/bom")
	public ResponseEntity<Void> registerBom(@RequestBody AddBomRequestDTO dto) {
		bomService.addBom(dto);
		return ResponseEntity.ok().build();
	}
	@GetMapping("/bom/bom/{parentId}")
	public BomListViewResponse getBomByParentId(@PathVariable("parentId") Long parentId) {
	    return bomService.getBomByParentItemId(parentId);
	}
	@PutMapping("/bom/bom")
	public ResponseEntity<Void> updateBom(@RequestBody UpdateBomRequestDTO dto) {
	    bomService.updateBom(dto);
	    return ResponseEntity.ok().build();
	}
	@DeleteMapping("/bom/bom")
	public ResponseEntity<Void> deleteBoms(@RequestBody List<Long> parentIds) {
	    bomService.deleteBomsByParentIds(parentIds);
	    return ResponseEntity.ok().build();
	}

}
