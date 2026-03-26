package kr.co.seoulit.his.admin.domain.master.code;

import kr.co.seoulit.his.admin.domain.master.code.dto.CodeItemCreateRequest;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeItemUpdateRequest;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeItemResponse;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeSetCreateRequest;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeSetResponse;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeSetUpdateRequest;
import kr.co.seoulit.his.admin.global.common.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/master/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeService service;

    @PostMapping("/sets")
    public CodeSet createCodeSet(@RequestBody @Valid CodeSetCreateRequest req) {
        return service.createCodeSet(req);
    }

    // 조회 표준화(검색/페이징/정렬) - 기존과 호환을 위해 /search로 추가
    @GetMapping("/sets/search")
    public PageResponse<CodeSetResponse> searchCodeSets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "codeSetKey", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return service.searchCodeSets(keyword, active, pageable);
    }

    @GetMapping("/sets/{codeSetKey}")
    public CodeSetResponse getCodeSet(@PathVariable String codeSetKey) {
        return service.getCodeSet(codeSetKey);
    }

    @PutMapping("/sets/{codeSetKey}")
    public CodeSetResponse updateCodeSet(@PathVariable String codeSetKey, @RequestBody @Valid CodeSetUpdateRequest req) {
        return service.updateCodeSet(codeSetKey, req);
    }

    // 물리삭제 대신 비활성화(현업형)
    @DeleteMapping("/sets/{codeSetKey}")
    public void deactivateCodeSet(@PathVariable String codeSetKey) {
        service.deactivateCodeSet(codeSetKey);
    }

    @PostMapping("/sets/{codeSetKey}/items")
    public CodeItemResponse addItem(@PathVariable String codeSetKey, @RequestBody @Valid CodeItemCreateRequest req) {
        return service.addItem(codeSetKey, req);
    }

    @PutMapping("/items/{codeItemId}")
    public CodeItemResponse updateItem(@PathVariable Long codeItemId, @RequestBody @Valid CodeItemUpdateRequest req) {
        return service.updateItem(codeItemId, req);
    }

    @DeleteMapping("/items/{codeItemId}")
    public void deactivateItem(@PathVariable Long codeItemId) {
        service.deactivateItem(codeItemId);
    }

    @GetMapping("/sets/{codeSetKey}/items")
    public List<CodeItemResponse> listItems(@PathVariable String codeSetKey) {
        return service.listItems(codeSetKey);
    }

    // 조회 표준화(검색/페이징/정렬)
    @GetMapping("/sets/{codeSetKey}/items/search")
    public PageResponse<CodeItemResponse> searchItems(
            @PathVariable String codeSetKey,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 50, sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return service.searchItems(codeSetKey, keyword, active, pageable);
    }
}
