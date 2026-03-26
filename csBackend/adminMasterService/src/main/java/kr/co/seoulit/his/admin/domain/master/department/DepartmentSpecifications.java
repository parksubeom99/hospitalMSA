package kr.co.seoulit.his.admin.domain.master.department;

import org.springframework.data.jpa.domain.Specification;

public final class DepartmentSpecifications {
    private DepartmentSpecifications() {}

    public static Specification<Department> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), like.toLowerCase()),
                    cb.like(cb.lower(root.get("name")), like.toLowerCase())
            );
        };
    }

    public static Specification<Department> active(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return cb.conjunction();
            return cb.equal(root.get("active"), active);
        };
    }
}
