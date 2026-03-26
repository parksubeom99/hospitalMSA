package kr.co.seoulit.hospital.iam.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/audit")
public class AuditQueryController {

    private final AuditLogRepository auditRepo;

    @GetMapping("/logs")
    public Page<AuditLog> search(
            @RequestParam(required = false) String actorLoginId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);

        Specification<AuditLog> spec =
                Specification.where(AuditLogSpecifications.actorLoginId(actorLoginId))
                        .and(AuditLogSpecifications.serviceName(serviceName))
                        .and(AuditLogSpecifications.action(action))
                        .and(AuditLogSpecifications.result(result))
                        .and(AuditLogSpecifications.targetType(targetType))
                        .and(AuditLogSpecifications.targetId(targetId))
                        .and(AuditLogSpecifications.patientId(patientId))
                        .and(AuditLogSpecifications.archived(archived))
                        .and(AuditLogSpecifications.keyword(keyword))
                        .and(AuditLogSpecifications.createdFrom(from))
                        .and(AuditLogSpecifications.createdTo(to));

        return auditRepo.findAll(spec, pageable);
    }

    private Sort parseSort(String sort) {
        // format: field,dir or "createdAt,desc"
        try {
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                return "asc".equalsIgnoreCase(parts[1])
                        ? Sort.by(parts[0]).ascending()
                        : Sort.by(parts[0]).descending();
            }
        } catch (Exception ignored) {}
        return Sort.by("createdAt").descending();
    }
}
