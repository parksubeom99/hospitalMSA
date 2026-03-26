package kr.co.seoulit.his.admin.global.common.page;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 조회 API 표준 응답(페이징/정렬/검색)을 위한 공통 래퍼
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
