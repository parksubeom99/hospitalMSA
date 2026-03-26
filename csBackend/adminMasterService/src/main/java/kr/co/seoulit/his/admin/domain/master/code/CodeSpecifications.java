package kr.co.seoulit.his.admin.domain.master.code;

import org.springframework.data.jpa.domain.Specification;

public final class CodeSpecifications {

    private CodeSpecifications() {}

    public static Specification<CodeSet> codeSetKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("codeSetKey")), like.toLowerCase()),
                    cb.like(cb.lower(root.get("name")), like.toLowerCase())
            );
        };
    }

    public static Specification<CodeSet> codeSetActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return cb.conjunction();
            return cb.equal(root.get("active"), active);
        };
    }

    public static Specification<CodeItem> codeItemKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), like.toLowerCase()),
                    cb.like(cb.lower(root.get("name")), like.toLowerCase())
            );
        };
    }

    public static Specification<CodeItem> codeItemActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return cb.conjunction();
            return cb.equal(root.get("active"), active);
        };
    }

    public static Specification<CodeItem> codeItemInSetKey(String codeSetKey) {
        return (root, query, cb) -> {
            if (codeSetKey == null || codeSetKey.isBlank()) return cb.conjunction();
            return cb.equal(root.join("codeSet").get("codeSetKey"), codeSetKey);
        };
    }
}
