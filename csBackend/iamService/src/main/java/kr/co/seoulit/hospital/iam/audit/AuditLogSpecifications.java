package kr.co.seoulit.hospital.iam.audit;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class AuditLogSpecifications {

    public static Specification<AuditLog> actorLoginId(String v) {
        return (root, q, cb) -> v == null || v.isBlank() ? null : cb.equal(root.get("actorLoginId"), v);
    }

    public static Specification<AuditLog> serviceName(String v) {
        return (root, q, cb) -> v == null || v.isBlank() ? null : cb.equal(root.get("serviceName"), v);
    }

    public static Specification<AuditLog> action(String v) {
        return (root, q, cb) -> v == null || v.isBlank() ? null : cb.equal(root.get("action"), v);
    }

    public static Specification<AuditLog> result(String v) {
        return (root, q, cb) -> v == null || v.isBlank() ? null : cb.equal(root.get("result"), v);
    }

    public static Specification<AuditLog> targetType(String v) {
        return (root, q, cb) -> v == null || v.isBlank() ? null : cb.equal(root.get("targetType"), v);
    }

    public static Specification<AuditLog> targetId(String v) {
        return (root, q, cb) -> v == null || v.isBlank() ? null : cb.equal(root.get("targetId"), v);
    }

    public static Specification<AuditLog> patientId(Long v) {
        return (root, q, cb) -> v == null ? null : cb.equal(root.get("patientId"), v);
    }

    public static Specification<AuditLog> createdFrom(LocalDateTime from) {
        return (root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> createdTo(LocalDateTime to) {
        return (root, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<AuditLog> archived(Boolean v) {
        return (root, q, cb) -> v == null ? null : cb.equal(root.get("archived"), v);
    }

    public static Specification<AuditLog> keyword(String kw) {
        return (root, q, cb) -> {
            if (kw == null || kw.isBlank()) return null;
            String like = "%" + kw.trim() + "%";
            return cb.like(root.get("detailJson"), like);
        };
    }
}
