package kr.co.seoulit.his.admin.domain.master.code.dto;

public record CodeSetResponse(
        Long codeSetId,
        String codeSetKey,
        String name,
        boolean active
) {
}
