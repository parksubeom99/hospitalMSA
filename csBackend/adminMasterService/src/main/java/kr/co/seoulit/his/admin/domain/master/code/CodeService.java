package kr.co.seoulit.his.admin.domain.master.code;

import kr.co.seoulit.his.admin.audit.MasterAuditClient;
import kr.co.seoulit.his.admin.global.common.page.PageResponse;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeItemCreateRequest;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeItemUpdateRequest;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeItemResponse;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeSetCreateRequest;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeSetResponse;
import kr.co.seoulit.his.admin.domain.master.code.dto.CodeSetUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeSetRepository codeSetRepo;
    private final CodeItemRepository codeItemRepo;
    private final MasterAuditClient auditClient;

    @Transactional
    public CodeSet createCodeSet(CodeSetCreateRequest req) {
        codeSetRepo.findByCodeSetKey(req.codeSetKey()).ifPresent(x -> {
            throw new IllegalArgumentException("CodeSet already exists: " + req.codeSetKey());
        });

        CodeSet saved = codeSetRepo.save(CodeSet.builder()
                .codeSetKey(req.codeSetKey())
                .name(req.name())
                .active(true)
                .build());

        auditClient.write("CODESET_CREATED", "CODE_SET", String.valueOf(saved.getCodeSetId()), null,
                java.util.Map.of("codeSetKey", saved.getCodeSetKey(), "name", saved.getName()));

        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<CodeSetResponse> searchCodeSets(String keyword, Boolean active, Pageable pageable) {
        Specification<CodeSet> spec = Specification
                .where(CodeSpecifications.codeSetKeyword(keyword))
                .and(CodeSpecifications.codeSetActive(active));

        Page<CodeSetResponse> page = codeSetRepo.findAll(spec, pageable)
                .map(this::toCodeSetResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public CodeSetResponse getCodeSet(String codeSetKey) {
        CodeSet set = codeSetRepo.findByCodeSetKey(codeSetKey)
                .orElseThrow(() -> new IllegalArgumentException("CodeSet not found: " + codeSetKey));
        return toCodeSetResponse(set);
    }

    @Transactional
    public CodeSetResponse updateCodeSet(String codeSetKey, CodeSetUpdateRequest req) {
        CodeSet set = codeSetRepo.findByCodeSetKey(codeSetKey)
                .orElseThrow(() -> new IllegalArgumentException("CodeSet not found: " + codeSetKey));

        set.setName(req.name());
        if (req.active() != null) set.setActive(req.active());

        CodeSet saved = codeSetRepo.save(set);
        auditClient.write("CODESET_UPDATED", "CODE_SET", String.valueOf(saved.getCodeSetId()), null,
                java.util.Map.of("codeSetKey", saved.getCodeSetKey(), "name", saved.getName(), "active", saved.isActive()));
        return toCodeSetResponse(saved);
    }

    @Transactional
    public void deactivateCodeSet(String codeSetKey) {
        CodeSet set = codeSetRepo.findByCodeSetKey(codeSetKey)
                .orElseThrow(() -> new IllegalArgumentException("CodeSet not found: " + codeSetKey));
        set.setActive(false);
        codeSetRepo.save(set);
        auditClient.write("CODESET_DEACTIVATED", "CODE_SET", String.valueOf(set.getCodeSetId()), null,
                java.util.Map.of("codeSetKey", set.getCodeSetKey()));
    }

    @Transactional
    public CodeItemResponse addItem(String codeSetKey, CodeItemCreateRequest req) {
        CodeSet set = codeSetRepo.findByCodeSetKey(codeSetKey)
                .orElseThrow(() -> new IllegalArgumentException("CodeSet not found: " + codeSetKey));

        CodeItem item = CodeItem.builder()
                .codeSet(set)
                .code(req.code())
                .name(req.name())
                .active(req.active() == null ? true : req.active())
                .sortOrder(req.sortOrder() == null ? 0 : req.sortOrder())
                .build();

        CodeItem saved = codeItemRepo.save(item);

        auditClient.write("CODEITEM_CREATED", "CODE_ITEM", String.valueOf(saved.getCodeItemId()), null,
                java.util.Map.of("codeSetKey", codeSetKey, "code", saved.getCode(), "name", saved.getName()));

        return toResponse(saved);
    }

    @Transactional
    public CodeItemResponse updateItem(Long codeItemId, CodeItemUpdateRequest req) {
        CodeItem item = codeItemRepo.findById(codeItemId)
                .orElseThrow(() -> new IllegalArgumentException("CodeItem not found: " + codeItemId));

        item.setName(req.name());
        if (req.active() != null) item.setActive(req.active());
        if (req.sortOrder() != null) item.setSortOrder(req.sortOrder());

        CodeItem saved = codeItemRepo.save(item);

        auditClient.write("CODEITEM_UPDATED", "CODE_ITEM", String.valueOf(saved.getCodeItemId()), null,
                java.util.Map.of("codeSetKey", saved.getCodeSet().getCodeSetKey(), "code", saved.getCode(), "name", saved.getName()));
        return toResponse(saved);
    }

    @Transactional
    public void deactivateItem(Long codeItemId) {
        CodeItem item = codeItemRepo.findById(codeItemId)
                .orElseThrow(() -> new IllegalArgumentException("CodeItem not found: " + codeItemId));
        item.setActive(false);
        codeItemRepo.save(item);

        auditClient.write("CODEITEM_DEACTIVATED", "CODE_ITEM", String.valueOf(item.getCodeItemId()), null,
                java.util.Map.of("codeSetKey", item.getCodeSet().getCodeSetKey(), "code", item.getCode()));
    }

    @Transactional(readOnly = true)
    public PageResponse<CodeItemResponse> searchItems(String codeSetKey, String keyword, Boolean active, Pageable pageable) {
        Specification<CodeItem> spec = Specification
                .where(CodeSpecifications.codeItemInSetKey(codeSetKey))
                .and(CodeSpecifications.codeItemKeyword(keyword))
                .and(CodeSpecifications.codeItemActive(active));

        Page<CodeItemResponse> page = codeItemRepo.findAll(spec, pageable)
                .map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<CodeItemResponse> listItems(String codeSetKey) {
        return codeItemRepo.findByCodeSet_CodeSetKeyOrderBySortOrderAsc(codeSetKey)
                .stream().map(this::toResponse).toList();
    }

    private CodeItemResponse toResponse(CodeItem item) {
        return new CodeItemResponse(
                item.getCodeItemId(),
                item.getCodeSet().getCodeSetKey(),
                item.getCode(),
                item.getName(),
                item.isActive(),
                item.getSortOrder()
        );
    }

    private CodeSetResponse toCodeSetResponse(CodeSet set) {
        return new CodeSetResponse(set.getCodeSetId(), set.getCodeSetKey(), set.getName(), set.isActive());
    }
}
