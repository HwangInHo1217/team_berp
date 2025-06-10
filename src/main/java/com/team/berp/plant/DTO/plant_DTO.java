package com.team.berp.plant.DTO;

import org.springframework.stereotype.Repository;

import lombok.Data;

@Data
@Repository("plant_DTO")
public class plant_DTO {
 
   //번호(자동 순번)
   private long work_id;
   
   //사업자 명, 사업자 대표, 사업자 번호, 업태
   private String work_name, work_ceonm, work_no, work_cond;
   
   //품목(종목), 전화번호, 팩스, 주소
   private String work_item, work_tel, work_fax;
   
   //도로명, 기본주소, 상세주소
   private String work_addr, work_mainadd, work_detailadd;
   
   //회사 담당자명, 이메일, 번호(직통번호)
   private String work_manname, work_manemail, work_mantel;
}