package com.cmms11.init;

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
import com.cmms11.domain.role.Role;
import com.cmms11.domain.role.RoleId;
import com.cmms11.domain.role.RoleRepository;
import com.cmms11.domain.func.Func;
import com.cmms11.domain.func.FuncId;
import com.cmms11.domain.func.FuncRepository;
import com.cmms11.domain.storage.Storage;
import com.cmms11.domain.storage.StorageId;
import com.cmms11.domain.storage.StorageRepository;
import com.cmms11.code.CodeType;
import com.cmms11.code.CodeTypeId;
import com.cmms11.code.CodeTypeRepository;
import com.cmms11.code.CodeItem;
import com.cmms11.code.CodeItemId;
import com.cmms11.code.CodeItemRepository;
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
    private static final String DEFAULT_COMPANY = "CHROK";
    private static final String SYSTEM_USER = "system";
  
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;
    private final DeptRepository deptRepository;
    private final RoleRepository roleRepository;
    private final FuncRepository funcRepository;
    private final StorageRepository storageRepository;

    private final CodeTypeRepository codeTypeRepository;
    private final CodeItemRepository codeItemRepository;

    public DataInitializer(
        MemberRepository memberRepository,
        PasswordEncoder passwordEncoder,
        CompanyRepository companyRepository,
        SiteRepository siteRepository,
        DeptRepository deptRepository,
        RoleRepository roleRepository,
        FuncRepository funcRepository,
        StorageRepository storageRepository,
        CodeTypeRepository codeTypeRepository,
        CodeItemRepository codeItemRepository
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyRepository = companyRepository;
        this.siteRepository = siteRepository;
        this.deptRepository = deptRepository;
        this.roleRepository = roleRepository;
        this.funcRepository = funcRepository;
        this.storageRepository = storageRepository;
        this.codeTypeRepository = codeTypeRepository;
        this.codeItemRepository = codeItemRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();
        seedAdmin(now);
        seedCompanyHierarchy(now);
        seedRoles(now);
        seedFuncs(now);
        seedStorages(now);
        seedCodes();
    }

    private void seedAdmin(LocalDateTime now) {
        // ⭐ 각 회사별 admin 계정 생성
        List<String> companyIds = List.of("CHROK", "HPS", "KEPS", "OES");
        
        companyIds.forEach(companyId -> {
            MemberId adminId = new MemberId(companyId, "admin");
            if (memberRepository.existsById(adminId)) {
                return;
            }
            
            Member admin = new Member();
            admin.setId(adminId);
            admin.setName("Administrator");
            admin.setDeptId("ADMIN");
            admin.setPasswordHash(passwordEncoder.encode("1234qwer!"));
            admin.setDeleteMark("N");
            admin.setCreatedAt(now);
            admin.setCreatedBy(SYSTEM_USER);
            admin.setUpdatedAt(now);
            admin.setUpdatedBy(SYSTEM_USER);
            memberRepository.save(admin);
        });
    }

    private void seedCompanyHierarchy(LocalDateTime now) {
        // ⭐ 여러 회사 데이터 시드
        List<SeedCompany> companies = List.of(
            new SeedCompany("CHROK", "초록에너지", "123-45-67890", "admin@chorokenergy.co.kr", "02-1234-5678"),
            new SeedCompany("HPS", "한국플랜트서비스", "234-56-78901", "admin@hps.co.kr", "02-1234-5678"),
            new SeedCompany("KEPS", "한국발전기술", "345-67-89012", "admin@keps.r", "02-1234-5678"),
            new SeedCompany("OES", "옵티멀에너지서비스", "345-67-89012", "admin@oes.kr", "02-1234-5678")
        );
        
        companies.forEach(seed -> {
            Company company = companyRepository.findById(seed.companyId()).orElseGet(Company::new);
            if (company.getCompanyId() == null) {
                company.setCompanyId(seed.companyId());
                company.setCreatedAt(now);
                company.setCreatedBy(SYSTEM_USER);
            }
            company.setName(seed.name());
            company.setBizNo(seed.bizNo());
            company.setEmail(seed.email());
            company.setPhone(seed.phone());
            company.setDeleteMark("N");
            company.setUpdatedAt(now);
            company.setUpdatedBy(SYSTEM_USER);
            companyRepository.save(company);
        });

        List<Site> sites = List.of(
            buildSite(now, "S0001", "Sample site1"),
            buildSite(now, "S0002", "Sample site2")
        );
        sites.forEach(siteRepository::save);

        List<Dept> depts = List.of(
            buildDept(now, "D0000", "Sample division1", null),
            buildDept(now, "D0001", "Sample department1", "D0000"),
            buildDept(now, "D0002", "Sample department2", "D0000")
        );
        depts.forEach(deptRepository::save);

        // Seed sample members
        List<Member> members = List.of(
            buildMember(now, "user1", "Sample User1", "S0001", "D0001"),
            buildMember(now, "user2", "Sample User2", "S0001", "D0002")
        );
        members.forEach(memberRepository::save);
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

    private Dept buildDept(LocalDateTime now, String deptId, String name, String parentDeptId) {
        DeptId id = new DeptId(DEFAULT_COMPANY, deptId);
        Dept dept = deptRepository.findById(id).orElseGet(Dept::new);
        dept.setId(id);
        if (dept.getCreatedAt() == null) {
            dept.setCreatedAt(now);
            dept.setCreatedBy(SYSTEM_USER);
        }
        dept.setName(name);
        dept.setParentId(parentDeptId);
        dept.setDeleteMark("N");
        dept.setUpdatedAt(now);
        dept.setUpdatedBy(SYSTEM_USER);
        return dept;
    }

    private Member buildMember(LocalDateTime now, String memberId, String name, String siteId, String deptId) {
        MemberId id = new MemberId(DEFAULT_COMPANY, memberId);
        Member member = memberRepository.findById(id).orElseGet(Member::new);
        member.setId(id);
        if (member.getCreatedAt() == null) {
            member.setCreatedAt(now);
            member.setCreatedBy(SYSTEM_USER);
        }
        member.setName(name);
        member.setSiteId(siteId);
        member.setDeptId(deptId);
        member.setPasswordHash(passwordEncoder.encode("1234"));
        member.setDeleteMark("N");
        member.setUpdatedAt(now);
        member.setUpdatedBy(SYSTEM_USER);
        return member;
    }

    private void seedRoles(LocalDateTime now) {
        List<SeedRole> roles = List.of(
            new SeedRole("ADMIN", "System Administrator", "시스템 관리자"),
            new SeedRole("MANGR", "Business Manager", "업무 관리자"),
            new SeedRole("ASSNT", "Business Assistant", "업무 보조자"),
            new SeedRole("PARTR", "Partner", "협력업체")
        );

        roles.forEach(seedRole -> {
            RoleId id = new RoleId(DEFAULT_COMPANY, seedRole.roleId());
            Role role = roleRepository.findById(id).orElseGet(Role::new);
            
            if (role.getId() == null) {
                role.setId(id);
                role.setCreatedAt(now);
                role.setCreatedBy(SYSTEM_USER);
            }
            
            role.setName(seedRole.name());
            role.setNote(seedRole.note());
            role.setDeleteMark("N");
            role.setUpdatedAt(now);
            role.setUpdatedBy(SYSTEM_USER);
            
            roleRepository.save(role);
        });
    }

    private void seedCodes() {
        Map<String, String> codeTypes = new LinkedHashMap<>();
        codeTypes.put("ASSET", "자산유형");
        codeTypes.put("JOBTP", "작업유형");
        codeTypes.put("PERMT", "허가유형");
        codeTypes.put("DEPRE", "감가유형");
        codeTypes.put("MODUL", "참조모듈");
        codeTypes.put("APPRV", "결재상태-STATUS:시스템 사용용도이므로 수정금지");
        codeTypes.put("DECSN", "결재자유형-DECISION:시스템 사용용도이므로 수정금지");

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
            new SeedCodeItem("INVNT", "재고자산"),
            new SeedCodeItem("TOOL", "공기구"),
            new SeedCodeItem("BUILD", "건축물"),
            new SeedCodeItem("ETC", "기타")
        ));

        seedItems("JOBTP", List.of(
            new SeedCodeItem("PLI01", "정기점검(Planned Inspection)"),
            new SeedCodeItem("UPI01", "돌발점검(Unplanned Inspection)"),
            new SeedCodeItem("PLW01", "정기작업(Planned Work)"),
            new SeedCodeItem("UPW01", "돌발작업(Unplanned Work)")
        ));

        seedItems("PERMT", List.of(
            new SeedCodeItem("PEWOK", "작업허가(Work Permit)"),
            new SeedCodeItem("PEINS", "점검허가(Inspection Permit)"),
            new SeedCodeItem("PEINP", "일반허가(General Permit)")
        ));

        seedItems("DEPRE", List.of(
            new SeedCodeItem("STRAI", "정액법(Straight-line)"),
            new SeedCodeItem("DECLI", "정률법(Declining Balance)"),
            new SeedCodeItem("SUM", "연수합법(Sum-of-the-years' digits)"),
            new SeedCodeItem("NONE", "감가없음(None)")
        ));

        seedItems("MODUL", List.of(
            new SeedCodeItem("PLANT", "설비"),
            new SeedCodeItem("INVNT", "재고"),
            new SeedCodeItem("INSP", "점검"),
            new SeedCodeItem("WORK", "작업지시"),
            new SeedCodeItem("WPER", "작업허가"),
            new SeedCodeItem("MEMO", "게시글"),
            new SeedCodeItem("APPRL", "결재")
        ));

        seedItems("APPRV", List.of(
            new SeedCodeItem("DRAFT", "기안"),
            new SeedCodeItem("SUBMT", "제출"),
            new SeedCodeItem("PROC", "처리중"),
            new SeedCodeItem("APPRV", "승인"),
            new SeedCodeItem("REJCT", "반려"),
            new SeedCodeItem("CNCL", "취소"),
            new SeedCodeItem("CMPLT", "결재없이확정건")
        ));

        seedItems("DECSN", List.of(
            new SeedCodeItem("APPRL", "결재"),
            new SeedCodeItem("AGREE", "합의"),
            new SeedCodeItem("INFO", "참조")
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

    private void seedFuncs(LocalDateTime now) {
        // ⭐ 각 회사별 기능위치 데이터 생성
        List<String> companyIds = List.of("CHROK", "HPS", "KEPS", "OES");
        List<SeedFunc> funcs = List.of(
            new SeedFunc("FUC01", "기능위치 1"),
            new SeedFunc("FUC02", "기능위치 2")
        );
        
        companyIds.forEach(companyId -> {
            funcs.forEach(seedFunc -> {
                FuncId id = new FuncId(companyId, seedFunc.funcId());
                Func func = funcRepository.findById(id).orElseGet(Func::new);
                
                if (func.getId() == null) {
                    func.setId(id);
                    func.setCreatedAt(now);
                    func.setCreatedBy(SYSTEM_USER);
                }
                
                func.setName(seedFunc.name());
                func.setDeleteMark("N");
                func.setUpdatedAt(now);
                func.setUpdatedBy(SYSTEM_USER);
                
                funcRepository.save(func);
            });
        });
    }

    private void seedStorages(LocalDateTime now) {
        // ⭐ 각 회사별 창고 데이터 생성
        List<String> companyIds = List.of("CHROK", "HPS", "KEPS", "OES");
        List<SeedStorage> storages = List.of(
            new SeedStorage("STG01", "창고 1"),
            new SeedStorage("STG02", "창고 2")
        );
        
        companyIds.forEach(companyId -> {
            storages.forEach(seedStorage -> {
                StorageId id = new StorageId(companyId, seedStorage.storageId());
                Storage storage = storageRepository.findById(id).orElseGet(Storage::new);
                
                if (storage.getId() == null) {
                    storage.setId(id);
                    storage.setCreatedAt(now);
                    storage.setCreatedBy(SYSTEM_USER);
                }
                
                storage.setName(seedStorage.name());
                storage.setDeleteMark("N");
                storage.setUpdatedAt(now);
                storage.setUpdatedBy(SYSTEM_USER);
                
                storageRepository.save(storage);
            });
        });
    }

    private record SeedCodeItem(String code, String name) {
    }

    private record SeedRole(String roleId, String name, String note) {
    }
    
    private record SeedCompany(String companyId, String name, String bizNo, String email, String phone) {
    }
    
    private record SeedFunc(String funcId, String name) {
    }
    
    private record SeedStorage(String storageId, String name) {
    }
}

