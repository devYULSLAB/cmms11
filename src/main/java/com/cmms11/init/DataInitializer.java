package com.cmms11.init;

import com.cmms11.code.CodeItem;
import com.cmms11.code.CodeItemId;
import com.cmms11.code.CodeItemRepository;
import com.cmms11.code.CodeType;
import com.cmms11.code.CodeTypeId;
import com.cmms11.code.CodeTypeRepository;
import com.cmms11.domain.company.Company;
import com.cmms11.domain.company.CompanyRepository;
import com.cmms11.domain.dept.Dept;
import com.cmms11.domain.dept.DeptId;
import com.cmms11.domain.dept.DeptRepository;
import com.cmms11.domain.member.Member;
import com.cmms11.domain.member.MemberId;
import com.cmms11.domain.member.MemberRepository;
import com.cmms11.domain.site.Site;
import com.cmms11.domain.site.SiteId;
import com.cmms11.domain.site.SiteRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 이름: DataInitializer
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 애플리케이션 기동 시 기본 데이터(Admin, 회사/사이트/부서, 공통코드)를 시드.
 */
@Component
public class DataInitializer implements ApplicationRunner {
    private static final String DEFAULT_COMPANY = "C0001";
    private static final String SYSTEM_USER = "system";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;
    private final DeptRepository deptRepository;
    private final CodeTypeRepository codeTypeRepository;
    private final CodeItemRepository codeItemRepository;

    public DataInitializer(
        MemberRepository memberRepository,
        PasswordEncoder passwordEncoder,
        CompanyRepository companyRepository,
        SiteRepository siteRepository,
        DeptRepository deptRepository,
        CodeTypeRepository codeTypeRepository,
        CodeItemRepository codeItemRepository
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyRepository = companyRepository;
        this.siteRepository = siteRepository;
        this.deptRepository = deptRepository;
        this.codeTypeRepository = codeTypeRepository;
        this.codeItemRepository = codeItemRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();
        seedAdmin(now);
        seedCompanyHierarchy(now);
        seedCodes();
    }

    private void seedAdmin(LocalDateTime now) {
        MemberId adminId = new MemberId(DEFAULT_COMPANY, "admin");
        if (memberRepository.existsById(adminId)) {
            return;
        }
        Member admin = new Member();
        admin.setId(adminId);
        admin.setName("Administrator");
        admin.setDeptId("ADMIN");
        admin.setPasswordHash(passwordEncoder.encode("1234"));
        admin.setDeleteMark("N");
        admin.setCreatedAt(now);
        admin.setCreatedBy(SYSTEM_USER);
        admin.setUpdatedAt(now);
        admin.setUpdatedBy(SYSTEM_USER);
        memberRepository.save(admin);
    }

    private void seedCompanyHierarchy(LocalDateTime now) {
        Company company = companyRepository.findById(DEFAULT_COMPANY).orElseGet(Company::new);
        if (company.getCompanyId() == null) {
            company.setCompanyId(DEFAULT_COMPANY);
            company.setCreatedAt(now);
            company.setCreatedBy(SYSTEM_USER);
        }
        company.setName("Sample Company");
        company.setDeleteMark("N");
        company.setUpdatedAt(now);
        company.setUpdatedBy(SYSTEM_USER);
        companyRepository.save(company);

        List<Site> sites = List.of(
            buildSite(now, "S0001", "Sample site1"),
            buildSite(now, "S0002", "Sample site2")
        );
        sites.forEach(siteRepository::save);

        List<Dept> depts = List.of(
            buildDept(now, "D0001", "Sample department1"),
            buildDept(now, "D0002", "Sample department2")
        );
        depts.forEach(deptRepository::save);
    }

    private Site buildSite(LocalDateTime now, String siteId, String name) {
        SiteId id = new SiteId(DEFAULT_COMPANY, siteId);
        Site site = siteRepository.findById(id).orElseGet(Site::new);
        site.setId(id);
        if (site.getCreatedAt() == null) {
            site.setCreatedAt(now);
            site.setCreatedBy(SYSTEM_USER);
        }
        site.setName(name);
        site.setDeleteMark("N");
        site.setUpdatedAt(now);
        site.setUpdatedBy(SYSTEM_USER);
        return site;
    }

    private Dept buildDept(LocalDateTime now, String deptId, String name) {
        DeptId id = new DeptId(DEFAULT_COMPANY, deptId);
        Dept dept = deptRepository.findById(id).orElseGet(Dept::new);
        dept.setId(id);
        if (dept.getCreatedAt() == null) {
            dept.setCreatedAt(now);
            dept.setCreatedBy(SYSTEM_USER);
        }
        dept.setName(name);
        dept.setDeleteMark("N");
        dept.setUpdatedAt(now);
        dept.setUpdatedBy(SYSTEM_USER);
        return dept;
    }

    private void seedCodes() {
        Map<String, String> codeTypes = new LinkedHashMap<>();
        codeTypes.put("ASSET", "자산유형");
        codeTypes.put("JOBTP", "작업유형");
        codeTypes.put("PERMT", "허가유형");

        LocalDateTime now = LocalDateTime.now();
        codeTypes.forEach((codeType, name) -> {
            CodeType type = codeTypeRepository.findById(new CodeTypeId(DEFAULT_COMPANY, codeType))
                .orElseGet(CodeType::new);
            if (type.getId() == null) {
                type.setId(new CodeTypeId(DEFAULT_COMPANY, codeType));
                type.setCreatedAt(now);
                type.setCreatedBy(SYSTEM_USER);
            }
            type.setName(name);
            type.setDeleteMark("N");
            type.setUpdatedAt(now);
            type.setUpdatedBy(SYSTEM_USER);
            codeTypeRepository.save(type);
        });

        seedItems("ASSET", List.of(
            new SeedCodeItem("PLANT", "설비"),
            new SeedCodeItem("OFFIC", "사무용품"),
            new SeedCodeItem("INVET", "재고자산"),
            new SeedCodeItem("TOOL", "공기구"),
            new SeedCodeItem("BUILD", "건축물"),
            new SeedCodeItem("ETC", "기타")
        ));

        seedItems("JOBTP", List.of(
            new SeedCodeItem("PLI01", "정기점검"),
            new SeedCodeItem("UPI01", "돌발점검"),
            new SeedCodeItem("PLO01", "정기작업"),
            new SeedCodeItem("UPO01", "돌발작업")
        ));

        seedItems("PERMT", List.of(
            new SeedCodeItem("P0001", "허가1")
        ));
    }

    private void seedItems(String codeType, List<SeedCodeItem> items) {
        items.forEach(item -> {
            CodeItemId id = new CodeItemId(DEFAULT_COMPANY, codeType, item.code());
            CodeItem entity = codeItemRepository.findById(id).orElseGet(CodeItem::new);
            entity.setId(id);
            entity.setName(item.name());
            entity.setNote(null);
            codeItemRepository.save(entity);
        });
    }

    private record SeedCodeItem(String code, String name) {
    }
}

