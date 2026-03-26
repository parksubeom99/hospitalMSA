package kr.co.seoulit.his.admin.domain.frontoffice.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueMetricsService {

    private final QueueRepository queues;

    @Value("${admin.queue.eta.default-minutes:7}")
    private int defaultMinutes;

    @Value("${admin.queue.eta.sample-size:20}")
    private int sampleSize;

    @Transactional(readOnly = true)
    public Metrics calc(String category) {
        String cat = (category == null || category.isBlank()) ? "FRONT" : category.trim().toUpperCase();

        int waiting = queues.findByCategoryAndStatus(cat, "WAITING").size();

        // 최근 DONE 티켓들로 평균 처리시간(issued->done) 계산
        List<QueueTicket> done = queues.findByCategoryAndStatus(cat, "DONE");
        done.sort(Comparator.comparing(QueueTicket::getDoneAt, Comparator.nullsLast(Comparator.reverseOrder())));
        int n = Math.min(sampleSize, done.size());

        int avg = defaultMinutes;
        if (n > 0) {
            long total = 0;
            int used = 0;
            for (int i = 0; i < n; i++) {
                QueueTicket t = done.get(i);
                if (t.getIssuedAt() != null && t.getDoneAt() != null) {
                    long m = Duration.between(t.getIssuedAt(), t.getDoneAt()).toMinutes();
                    if (m > 0 && m < 240) { // 이상치 컷(4시간 이상 제외)
                        total += m;
                        used++;
                    }
                }
            }
            if (used > 0) avg = (int) Math.max(1, total / used);
            n = used;
        }

        int est = waiting * avg;
        return new Metrics(cat, waiting, n, avg, est, LocalDateTime.now());
    }

    public record Metrics(
            String category,
            int waitingCount,
            int sampleSize,
            int avgServiceMinutes,
            int estimatedMinutesForNew,
            LocalDateTime calculatedAt
    ) {}
}
