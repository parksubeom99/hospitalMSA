package kr.co.seoulit.hospital.iam.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditRetentionJob {

    private final AuditLogRepository auditRepo;

    @Value("${audit.retention-days:180}")
    private int retentionDays;

    // 매일 03:10 실행(서버 시간 기준)
    @Scheduled(cron = "0 10 3 * * *")
    public void archiveOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        // 단순 구현: archived=false 이면서 cutoff 이전 데이터를 archived 처리
        List<AuditLog> old = auditRepo.findAll((root, q, cb) -> cb.and(
                cb.lessThan(root.get("createdAt"), cutoff),
                cb.isFalse(root.get("archived"))
        ));
        if (old.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (AuditLog a : old) {
            a.setArchived(true);
            a.setArchivedAt(now);
        }
        auditRepo.saveAll(old);
    }
}
