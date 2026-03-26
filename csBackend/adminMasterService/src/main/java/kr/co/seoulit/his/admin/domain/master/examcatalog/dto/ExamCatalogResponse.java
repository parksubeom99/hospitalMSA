package kr.co.seoulit.his.admin.domain.master.examcatalog.dto;

public record ExamCatalogResponse(
        Long examCatalogId,
        String itemCode,
        String category,
        String displayNameKr,
        boolean active
) {}
