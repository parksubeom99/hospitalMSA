package kr.co.seoulit.his.clinical.domain.emr.service;

import kr.co.seoulit.his.clinical.audit.AuditClient;
import kr.co.seoulit.his.clinical.global.response.CurrentUserUtil;
import kr.co.seoulit.his.clinical.domain.emr.EncounterNote;
import kr.co.seoulit.his.clinical.domain.emr.EncounterNoteRepository;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterArchiveRequest;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterCreateRequest;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterResponse;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterUpdateRequest;
import kr.co.seoulit.his.clinical.domain.emr.mapper.EncounterNoteMapper;
import kr.co.seoulit.his.clinical.global.exception.BusinessException;
import kr.co.seoulit.his.clinical.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EncounterNoteService {

    private final EncounterNoteRepository repo;
    private final EncounterNoteMapper mapper;
    private final AuditClient audit;

    @Transactional
    public EncounterResponse create(EncounterCreateRequest req) {
        EncounterNote e = EncounterNote.builder()
                .visitId(req.visitId())
                .note(req.note())
                .createdAt(LocalDateTime.now())
                .build();

        EncounterNote saved = repo.save(e);

        // ✅ 비CRUD(감사로그) - 컨트롤러 밖으로 이동
        audit.write("EMR_WRITTEN", "ENCOUNTER_NOTE", String.valueOf(saved.getNoteId()), null,
                Map.of("visitId", saved.getVisitId()));

        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> listByVisit(Long visitId, boolean includeArchived) {
        if (includeArchived) {
            return mapper.toResponseList(repo.findByVisitId(visitId));
        }
        return mapper.toResponseList(repo.findByVisitIdAndArchivedFalse(visitId));
    }

    @Transactional
    public EncounterResponse update(Long noteId, EncounterUpdateRequest req) {
        EncounterNote e = repo.findByNoteIdAndArchivedFalse(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Encounter note not found. noteId=" + noteId));

        e.setNote(req.note());
        e.setUpdatedAt(LocalDateTime.now());
        e.setUpdatedBy(CurrentUserUtil.currentLoginIdOrNull());

        EncounterNote saved = repo.save(e);

        audit.write("ENCOUNTER_UPDATED", "ENCOUNTER_NOTE", String.valueOf(saved.getNoteId()), null,
                Map.of("visitId", saved.getVisitId()));

        return mapper.toResponse(saved);
    }

    @Transactional
    public EncounterResponse archive(Long noteId, EncounterArchiveRequest req) {
        EncounterNote e = repo.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Encounter note not found. noteId=" + noteId));

        if (e.isArchived()) {
            return mapper.toResponse(e);
        }

        e.setArchived(true);
        e.setArchivedAt(LocalDateTime.now());
        e.setArchivedBy(CurrentUserUtil.currentLoginIdOrNull());
        e.setArchivedReason(req == null ? null : req.reason());
        e.setUpdatedAt(LocalDateTime.now());
        e.setUpdatedBy(CurrentUserUtil.currentLoginIdOrNull());

        EncounterNote saved = repo.save(e);

        audit.write("ENCOUNTER_ARCHIVED", "ENCOUNTER_NOTE", String.valueOf(saved.getNoteId()), null,
                Map.of("visitId", saved.getVisitId(), "archived", true));

        return mapper.toResponse(saved);
    }
}
