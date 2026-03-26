package kr.co.seoulit.his.admin.domain.master.staff;

import org.springframework.data.jpa.domain.Specification;

public final class StaffSpecifications {
    private StaffSpecifications() {}

    public static Specification<StaffProfile> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("loginId")), like.toLowerCase()),
                    cb.like(cb.lower(root.get("name")), like.toLowerCase())
            );
        };
    }

    public static Specification<StaffProfile> active(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return cb.conjunction();
            return cb.equal(root.get("active"), active);
        };
    }

    public static Specification<StaffProfile> jobType(String jobType) {
        return (root, query, cb) -> {
            if (jobType == null || jobType.isBlank()) return cb.conjunction();
            return cb.equal(cb.upper(root.get("jobType")), jobType.trim().toUpperCase());
        };
    }

    public static Specification<StaffProfile> departmentId(Long departmentId) {
        return (root, query, cb) -> {
            if (departmentId == null) return cb.conjunction();
            return cb.equal(root.get("departmentId"), departmentId);
        };
    }
}
