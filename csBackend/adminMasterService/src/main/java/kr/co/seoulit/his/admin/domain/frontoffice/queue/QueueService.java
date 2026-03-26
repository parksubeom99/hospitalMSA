package kr.co.seoulit.his.admin.domain.frontoffice.queue;

import kr.co.seoulit.his.admin.audit.AuditClient;
import kr.co.seoulit.his.admin.global.exception.BusinessException;
import kr.co.seoulit.his.admin.global.exception.ErrorCode;
import kr.co.seoulit.his.admin.messaging.kafka.producer.QueueEventPublisher;
import kr.co.seoulit.his.admin.domain.frontoffice.queue.dto.QueueCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.queue.dto.QueueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queues;
    private final AuditClient audit;

    private final QueueEventPublisher publisher;

    @Transactional
    public QueueResponse create(QueueCreateRequest req) {
        String category = normalizeCategory(req.category());
        QueueTicket ticket = ensureTicket(req.visitId(), category, "WAITING");

        audit.write("QUEUE_ISSUED", "QUEUE", String.valueOf(ticket.getTicketId()), null,
                Map.of("category", ticket.getCategory(), "status", ticket.getStatus()));

        return toResponse(ticket);
    }

    /**
     * Visit 생성/이벤트 반영 시 사용: (visitId, category) 티켓이 없으면 생성, 있으면 반환
     */
    @Transactional
    public QueueTicket ensureTicket(Long visitId, String category, String initialStatus) {
        String cat = normalizeCategory(category);
        return queues.findTopByVisitIdAndCategoryOrderByTicketIdDesc(visitId, cat)
                .orElseGet(() -> queues.save(QueueTicket.builder()
                        .visitId(visitId)
                        .category(cat)
                        .ticketNo(generateTicketNo())
                        .status(initialStatus == null ? "WAITING" : initialStatus)
                        .issuedAt(LocalDateTime.now())
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> list(String status, String category) {
        List<QueueTicket> list;
        if (category != null && !category.isBlank() && status != null && !status.isBlank()) {
            list = queues.findByCategoryAndStatus(normalizeCategory(category), status.trim().toUpperCase());
        } else if (category != null && !category.isBlank()) {
            list = queues.findByCategory(normalizeCategory(category));
        } else if (status != null && !status.isBlank()) {
            list = queues.findByStatus(status.trim().toUpperCase());
        } else {
            list = queues.findAll();
        }
        return list.stream().map(this::toResponse).toList();
    }

    // 워크플로우: 호출
    @Transactional
    public QueueResponse call(Long ticketId) {
        QueueTicket t = queues.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Queue ticket not found. ticketId=" + ticketId));

        String st = norm(t.getStatus());
        if (!"WAITING".equals(st)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only WAITING ticket can be called. status=" + st);
        }

        t.setStatus("CALLED");
        t.setCalledAt(LocalDateTime.now());
        QueueTicket saved = queues.save(t);

        audit.write("QUEUE_CALLED", "QUEUE", String.valueOf(saved.getTicketId()), null,
                Map.of("category", saved.getCategory(), "status", saved.getStatus()));

        publish("QUEUE_CALLED", saved.getVisitId(), saved.getCategory(), saved.getTicketId(), null);
        return toResponse(saved);
    }

    // 워크플로우: 완료
    @Transactional
    public QueueResponse done(Long ticketId) {
        QueueTicket t = queues.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Queue ticket not found. ticketId=" + ticketId));

        String st = norm(t.getStatus());
        if (!List.of("CALLED", "IN_PROGRESS").contains(st)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only CALLED/IN_PROGRESS ticket can be done. status=" + st);
        }

        t.setStatus("DONE");
        t.setDoneAt(LocalDateTime.now());
        QueueTicket saved = queues.save(t);

        audit.write("QUEUE_DONE", "QUEUE", String.valueOf(saved.getTicketId()), null,
                Map.of("category", saved.getCategory(), "status", saved.getStatus()));

        publish("QUEUE_COMPLETED", saved.getVisitId(), saved.getCategory(), saved.getTicketId(), null);
        return toResponse(saved);
    }

    /**
     * Kafka 이벤트 반영용(중복 방지 최소 형태: lastEventId로 동일 이벤트 중복 적용 방지)
     */
    @Transactional
    public void applyEvent(String eventId, String eventType, Long visitId, String category, String mappedStatus) {
        QueueTicket t = ensureTicket(visitId, category, "WAITING");

        if (eventId != null && eventId.equals(t.getLastEventId())) {
            return; // duplicate
        }

        String to = norm(mappedStatus);
        if (to.isBlank()) return;

        if ("IN_PROGRESS".equals(to)) {
            if ("WAITING".equals(norm(t.getStatus()))) {
                t.setCalledAt(LocalDateTime.now());
            }
            t.setStatus("IN_PROGRESS");
        } else if ("DONE".equals(to)) {
            if (t.getCalledAt() == null) t.setCalledAt(LocalDateTime.now());
            t.setStatus("DONE");
            t.setDoneAt(LocalDateTime.now());
        } else if ("CALLED".equals(to)) {
            t.setStatus("CALLED");
            t.setCalledAt(LocalDateTime.now());
        }

        t.setLastEventId(eventId);
        queues.save(t);

        audit.write("QUEUE_EVENT_APPLIED", "QUEUE", String.valueOf(t.getTicketId()), null,
                Map.of("eventType", eventType == null ? "" : eventType, "category", t.getCategory(), "status", t.getStatus()));
    }

    private void publish(String eventType, Long visitId, String category, Long ticketId, Long orderId) {
        try {
            publisher.publish(eventType, visitId, category, ticketId, orderId);
        } catch (Exception ignore) {
            // Kafka 미가동 시에도 원무 자체 기능은 계속 동작하도록(포트폴리오 데모 안정성)
        }
    }

    private QueueResponse toResponse(QueueTicket t) {
        return new QueueResponse(
                t.getTicketId(),
                t.getVisitId(),
                t.getCategory(),
                t.getTicketNo(),
                t.getStatus(),
                t.getIssuedAt(),
                t.getCalledAt(),
                t.getDoneAt()
        );
    }

    private String generateTicketNo() {
        return "Q-" + System.currentTimeMillis();
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private static String normalizeCategory(String s) {
        if (s == null || s.isBlank()) return "FRONT";
        return s.trim().toUpperCase();
    }
}
