package com.cmms11.common.seq;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * 자동 번호 생성 서비스
 * 
 * REQUIRES_NEW 트랜잭션 사용 이유:
 * - sequence 테이블의 pessimistic lock을 즉시 해제하기 위함
 * - 부모 트랜잭션(예: 파일 업로드)이 오래 걸릴 경우 lock이 지속되어 deadlock 발생 가능
 * - 독립 트랜잭션으로 ID 생성 후 즉시 커밋하여 lock 시간을 밀리초 단위로 최소화
 */
@Service
public class AutoNumberService {
    private final SequenceRepository repository;

    public AutoNumberService(SequenceRepository repository) {
        this.repository = repository;
    }

    /**
     * Master ID 생성: {moduleCode(1)}{9-digit seq}, dateKey fixed to '000000'
     * 독립 트랜잭션으로 실행되어 sequence lock 즉시 해제
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateMasterId(String companyId, String moduleCode) {
        String dateKey = "000000";
        int seq = getNextSequence(companyId, moduleCode, dateKey);
        return moduleCode + String.format("%09d", seq);
    }

    /**
     * Transactional ID 생성: {moduleCode(1)}{YYMMDD}{3-digit seq}
     * 독립 트랜잭션으로 실행되어 sequence lock 즉시 해제
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateTxId(String companyId, String moduleCode, LocalDate date) {
        LocalDate dateKey = date == null ? LocalDate.now() : date;
        String yymmdd = String.format("%1$ty%1$tm%1$td", dateKey);
        int seq = getNextSequence(companyId, moduleCode, yymmdd);
        return moduleCode + yymmdd + String.format("%03d", seq);
    }

    /**
     * 다음 시퀀스 번호 조회 및 증가
     * pessimistic lock(SELECT FOR UPDATE)을 사용하여 동시성 제어
     * 
     * @param companyId 회사 ID
     * @param moduleCode 모듈 코드 (F, M 등)
     * @param dateKey 날짜 키 (YYMMDD 또는 "000000")
     * @return 현재 시퀀스 번호 (DB는 +1 증가된 값으로 업데이트됨)
     */
    private int getNextSequence(String companyId, String moduleCode, String dateKey) {
        Sequence seq = repository.findForUpdate(companyId, moduleCode, dateKey)
            .orElseGet(() -> {
                Sequence s = new Sequence();
                s.setId(new SequenceId(companyId, moduleCode, dateKey));
                s.setNextSeq(1); 
                return repository.save(s);
            });
        int next = (seq.getNextSeq() == null ? 1 : seq.getNextSeq());
        seq.setNextSeq(next + 1);
        repository.save(seq);
        return next;
    }
}
