package com.team.berp.warehouse.util;


import java.util.Map;
import java.util.function.Supplier;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * API 응답 처리 공통 유틸리티 클래스
 * - 컨트롤러 단에서 공통적인 try-catch 로직을 줄이기 위해 사용
 * - 성공/실패에 따른 응답 상태 코드와 메시지를 일관성 있게 반환
 */
public class ApiUtils {

    private static final Logger log = LoggerFactory.getLogger(ApiUtils.class);

    /**
     * 일반적인 API 응답 처리용
     * - 성공 시 200 OK + 결과 데이터 반환
     * - IllegalArgumentException 발생 시 400 Bad Request + 메시지 반환 (JSON Map)
     * - 그 외 예외 시 500 Internal Server Error
     */
	public static <T> ResponseEntity<T> handle(Supplier<T> supplier) {
		try {
			T result = supplier.get();
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException e) {
			log.warn("요청 처리 실패: {}", e.getMessage());

			// 예외 메시지를 포함한 JSON 형태의 응답을 위해 Map으로 변환 후 캐스팅
	        @SuppressWarnings("unchecked")
	        T errorBody = (T) Map.of("message", e.getMessage());

			
			return ResponseEntity.badRequest().body(errorBody);
		} catch (Exception e) {
			log.error("서버 오류 발생", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
	
    /**
     * 리소스 생성 API 응답 처리용 (POST 등)
     * - 성공 시 201 Created + 결과 데이터
     * - IllegalArgumentException 발생 시 400 Bad Request
     * - 그 외 예외 시 500 Internal Server Error
     */
    public static <T> ResponseEntity<T> handleCreate(Supplier<T> supplier) {
        try {
            T result = supplier.get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("생성 요청 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("생성 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 삭제 API 응답 처리용 (DELETE 등)
     * - 성공 시 204 No Content
     * - RuntimeException 발생 시 404 Not Found
     * - 그 외 예외 시 500 Internal Server Error
     */
    public static ResponseEntity<Void> handleDelete(Runnable action) {
        try {
            action.run();
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("삭제 대상 없음: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("삭제 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 단건 조회용 응답 처리
     * - 성공 시 200 OK + 결과 데이터
     * - RuntimeException 발생 시 404 Not Found
     * - 그 외 예외 시 500 Internal Server Error
     */
    public static <T> ResponseEntity<T> handleGetById(Supplier<T> supplier) {
        try {
            T result = supplier.get();
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("조회 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
