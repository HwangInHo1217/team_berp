package com.team.berp.plant.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.team.berp.plant.DTO.plant_DTO;
import com.team.berp.plant.mapper.plant_DAO;
import com.team.berp.plant.service.plant_service;

import jakarta.annotation.Resource;

@Controller
public class plant_controller {
 
//	@Resource(name="plant_DTO")
//	plant_DTO dto;
	
	@Autowired
	private plant_service ps; //plant_service 클래스 자동 주입, DB 처리 담당
	
	/*Model m: 클라이언트에게 전달할 데이터를 저장하는 객체
	 * */
	
	@GetMapping("/pages/plant.html") //url 경로 - 해당 경로로 접속하면 실행됨
	public String plant(@RequestParam(value = "workplace_id", required = false) String workplace_id, Model m) {
		List<plant_DTO> list = ps.workplace_list(workplace_id); //특정 id로 리스트 조회
		
		  // 파라미터가 안 넘어왔을 경우 전체 목록 조회
	    if (workplace_id == null || workplace_id.isEmpty()) {
	        list = ps.workplace_list_all();  // 💡 이 메서드를 추가해야 함!
	    } else {
	        list = ps.workplace_list(workplace_id);
	    }
		
		m.addAttribute("list", list);
		
		System.out.println("넘어온 workplace_id = " + workplace_id);
		System.out.println("조회된 리스트 개수 = " + list.size());
		
		return "plant/plant"; //파일 경로 - src/main/resources/templates/plant/plant(.html)
	}
	
	@PostMapping("/plant/insert") //해당 경로로 요청이 오면 실행됨
	public String insertWorkplace(@ModelAttribute plant_DTO dto) { //form에서 넘어온 데이터를 자동으로 dto에 저장
		ps.workplace_info(dto); //service클래스에서 db에 dto값을 저장 
		//service -> dao -> mapper.java -> xml -> db
		return "redirect:/pages/plant.html";
		//완료 후 해당 페이지로 리다이렉트
	}
	
	@PostMapping("/plant/delete")
	public String deleteworkplace(@RequestParam(value="workplace_id") long workplace_id) {
		 ps.workplace_list_del(workplace_id);
		return "redirect:/pages/plant.html";
	}
	
}
