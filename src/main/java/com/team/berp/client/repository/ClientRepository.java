package com.team.berp.client.repository;

import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Company, Long> {

    // --------------------------------------------------------------------------------
    // 1) “useYn” 상관없이 중복 검사용 메서드 (정지된 거래처(N)까지 모두 포함)
    // --------------------------------------------------------------------------------
    boolean existsByCompanyNameAndCompanyNo(String companyName, String companyNo);
    boolean existsByCompanyNameAndCompanyNoAndCompanyIdNot(String companyName, String companyNo, Long excludeCompanyId);

    boolean existsByCompanyNo(String companyNo);
    boolean existsByCompanyNoAndCompanyIdNot(String companyNo, Long excludeCompanyId);

    boolean existsByCompanyName(String companyName);
    boolean existsByCompanyNameAndCompanyIdNot(String companyName, Long companyId);

    // --------------------------------------------------------------------------------
    // 2) 기존 “활성(useYn='Y') 기준 중복 검사” 메서드 (필요에 따라 남겨두었습니다)
    // --------------------------------------------------------------------------------
    boolean existsByCompanyNameAndCompanyNoAndUseYn(
        String companyName,
        String companyNo,
        String useYn
    );
    boolean existsByCompanyNameAndCompanyNoAndCompanyIdNotAndUseYn(
        String companyName,
        String companyNo,
        Long excludeCompanyId,
        String useYn
    );
    boolean existsByCompanyNoAndUseYn(String companyNo, String useYn);
    boolean existsByCompanyNoAndCompanyIdNotAndUseYn(
        String companyNo,
        Long excludeCompanyId,
        String useYn
    );
    boolean existsByCompanyNameAndUseYn(String companyName, String useYn);
    boolean existsByCompanyNameAndCompanyIdNotAndUseYn(
        String companyName,
        Long companyId,
        String useYn
    );

    // --------------------------------------------------------------------------------
    // 3) “useYn” 필터 없이 조회하는 메서드 (정지 상태(N) 포함 전체 조회)
    // --------------------------------------------------------------------------------
    // ※ JpaRepository에서 이미 제공되는 findAll(Pageable) 을 명시적으로 다시 적어둘 수 있지만,
    //    여기서는 주석 처리해두었습니다.
    // Page<Company> findAll(Pageable pageable);

    // ■ 키워드 검색 (페이징)
    Page<Company> findByCompanyNameContaining(String name, Pageable pageable);
    Page<Company> findByPresidentNmContaining(String presidentNm, Pageable pageable);
    Page<Company> findByCompanyNoContaining(String companyNo, Pageable pageable);
    Page<Company> findByEmployee_EmpNameContaining(String empName, Pageable pageable);

    // ■ 유형별 전체 조회 (페이징)
    Page<Company> findByCompanyType(CompanyType type, Pageable pageable);

    // ■ 유형 + 키워드 검색 (페이징)
    Page<Company> findByCompanyTypeAndCompanyNameContaining(CompanyType type, String name, Pageable pageable);
    Page<Company> findByCompanyTypeAndPresidentNmContaining(CompanyType type, String presidentNm, Pageable pageable);
    Page<Company> findByCompanyTypeAndCompanyNoContaining(CompanyType type, String companyNo, Pageable pageable);
    Page<Company> findByCompanyTypeAndEmployee_EmpNameContaining(CompanyType type, String empName, Pageable pageable);

    // --------------------------------------------------------------------------------
    // 4) 기존 “활성(useYn='Y') 기준 조회” 메서드 (필요에 따라 남겨두었습니다)
    // --------------------------------------------------------------------------------
    Page<Company> findByUseYn(String useYn, Pageable pageable);

    Page<Company> findByCompanyNameContainingAndUseYn(
        String name,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByPresidentNmContainingAndUseYn(
        String ceo,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByCompanyNoContainingAndUseYn(
        String bizNum,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByEmployee_EmpNameContainingAndUseYn(
        String empName,
        String useYn,
        Pageable pageable
    );

    Page<Company> findByCompanyTypeAndUseYn(
        CompanyType companyType,
        String useYn,
        Pageable pageable
    );

    Page<Company> findByCompanyTypeAndCompanyNameContainingAndUseYn(
        CompanyType companyType,
        String name,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByCompanyTypeAndPresidentNmContainingAndUseYn(
        CompanyType companyType,
        String ceo,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByCompanyTypeAndCompanyNoContainingAndUseYn(
        CompanyType companyType,
        String bizNum,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByCompanyTypeAndEmployee_EmpNameContainingAndUseYn(
        CompanyType companyType,
        String empName,
        String useYn,
        Pageable pageable
    );
}
