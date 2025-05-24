package com.team.berp.warehouse.util;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WhsJsonUtil
 * 
 * 창고 관리에서 사용하는 JSON 변환 유틸리티 클래스
 * - Thymeleaf에서 Java 객체를 HTML 태그 속성에 JSON 문자열로 삽입할 수 있도록 지원
 * - 주로 수정 버튼의 data-whs 속성에 사용됨
 */
@Component("whsJson")
public class WhsJsonUtil {
    
    /**
     * Jackson의 ObjectMapper 인스턴스
     * - Spring Boot가 자동으로 Bean으로 등록해주는 것을 주입받아 사용
     */
    private final ObjectMapper objectMapper;
    
    /**
     * 생성자 주입 방식
     * @param objectMapper JSON 직렬화를 위한 ObjectMapper
     */
    public WhsJsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 객체를 JSON 문자열로 변환
     * - 주로 WarehouseResponseDTO 객체를 JSON으로 변환하여 화면에 넘길 때 사용
     * - 변환 실패 시 '{}'를 반환하여 JS 측 오류 방지
     * @param obj 변환할 Java 객체 (주로 WarehouseResponseDTO)
     * @return JSON 문자열 (변환 실패 시 빈 객체 "{}")
     * 
     * 동작 방식:
     * 1. objectMapper.writeValueAsString()으로 객체 직렬화
     * 2. 성공 시 완전한 JSON 문자열 반환
     * 3. 실패 시 "{}" 반환하여 JavaScript 파싱 오류 방지
     * 예시 사용위치(thymeleaf)
     * <button th:attr="data-whs=${@whsJson.toJson(whs)}">수정</button>
     */
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
        	// 예외 발생 시 빈 JSON 객체 문자열 반환 → JS parse 시 안전하게 처리
            return "{}";
        }
    }
}