package kr.co.seoulit.his.support.messaging.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final SupportOutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${support.queue.topic}")
    private String topic;

    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.publisher.max-retries:10}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:2000}")
    @Transactional
    public void publishPending() {
        List<SupportOutboxEvent> pending = repo.findByStatusOrderByIdAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        for (SupportOutboxEvent e : pending) {
            try {
                // key=aggregateId(orderId) 로 파티셔닝 안정
                kafkaTemplate.send(topic, e.getAggregateId(), e.getPayload()).get();
                e.setStatus(OutboxStatus.PUBLISHED);
                e.setPublishedAt(LocalDateTime.now());
            } catch (Exception ex) {
                int next = e.getRetryCount() + 1;
                e.setRetryCount(next);
                if (next >= maxRetries) {
                    e.setStatus(OutboxStatus.FAILED);
                }
            }
        }
    }
}
