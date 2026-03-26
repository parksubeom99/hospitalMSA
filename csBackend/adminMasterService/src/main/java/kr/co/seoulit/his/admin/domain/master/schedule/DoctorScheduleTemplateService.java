package kr.co.seoulit.his.admin.domain.master.schedule;

import kr.co.seoulit.his.admin.audit.MasterAuditClient;
import kr.co.seoulit.his.admin.global.common.page.PageResponse;
import kr.co.seoulit.his.admin.domain.master.schedule.dto.ScheduleTemplateCreateRequest;
import kr.co.seoulit.his.admin.domain.master.schedule.dto.ScheduleTemplateResponse;
import kr.co.seoulit.his.admin.domain.master.schedule.dto.ScheduleTemplateUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorScheduleTemplateService {

    private final DoctorScheduleTemplateRepository repo;
    private final MasterAuditClient auditClient;

    @Transactional
    public ScheduleTemplateResponse create(ScheduleTemplateCreateRequest req) {
        validateRange(req.dayOfWeek(), req.startTime(), req.endTime());
        DoctorScheduleTemplate saved = repo.save(DoctorScheduleTemplate.builder()
                .staffProfileId(req.staffProfileId())
                .dayOfWeek(req.dayOfWeek())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .slotMinutes(req.slotMinutes() == null ? 10 : req.slotMinutes())
                .active(true)
                .note(req.note())
                .build());

        auditClient.write("SCHEDULE_TEMPLATE_CREATED", "DOCTOR_SCHEDULE_TEMPLATE", String.valueOf(saved.getScheduleTemplateId()), null,
                Map.of("staffProfileId", saved.getStaffProfileId(), "dayOfWeek", saved.getDayOfWeek()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduleTemplateResponse> listActive(Long staffProfileId) {
        return repo.findByStaffProfileIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(staffProfileId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleTemplateResponse> search(Long staffProfileId, Pageable pageable) {
        return PageResponse.from(repo.findByStaffProfileId(staffProfileId, pageable).map(this::toResponse));
    }

    @Transactional
    public ScheduleTemplateResponse update(Long id, ScheduleTemplateUpdateRequest req) {
        DoctorScheduleTemplate t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ScheduleTemplate not found: " + id));

        Integer day = req.dayOfWeek() != null ? req.dayOfWeek() : t.getDayOfWeek();
        var start = req.startTime() != null ? req.startTime() : t.getStartTime();
        var end = req.endTime() != null ? req.endTime() : t.getEndTime();
        validateRange(day, start, end);

        if (req.dayOfWeek() != null) t.setDayOfWeek(req.dayOfWeek());
        if (req.startTime() != null) t.setStartTime(req.startTime());
        if (req.endTime() != null) t.setEndTime(req.endTime());
        if (req.slotMinutes() != null) t.setSlotMinutes(req.slotMinutes());
        if (req.active() != null) t.setActive(req.active());
        if (req.note() != null) t.setNote(req.note());

        DoctorScheduleTemplate saved = repo.save(t);
        auditClient.write("SCHEDULE_TEMPLATE_UPDATED", "DOCTOR_SCHEDULE_TEMPLATE", String.valueOf(saved.getScheduleTemplateId()), null,
                Map.of("staffProfileId", saved.getStaffProfileId(), "dayOfWeek", saved.getDayOfWeek(), "active", saved.isActive()));
        return toResponse(saved);
    }

    // 물리삭제 대신 비활성화(현업형)
    @Transactional
    public void deactivate(Long id) {
        DoctorScheduleTemplate t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ScheduleTemplate not found: " + id));
        t.setActive(false);
        repo.save(t);
        auditClient.write("SCHEDULE_TEMPLATE_DEACTIVATED", "DOCTOR_SCHEDULE_TEMPLATE", String.valueOf(t.getScheduleTemplateId()), null,
                Map.of("staffProfileId", t.getStaffProfileId(), "dayOfWeek", t.getDayOfWeek()));
    }

    private void validateRange(Integer dayOfWeek, java.time.LocalTime start, java.time.LocalTime end) {
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be 1..7");
        }
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
    }

    private ScheduleTemplateResponse toResponse(DoctorScheduleTemplate t) {
        return new ScheduleTemplateResponse(
                t.getScheduleTemplateId(),
                t.getStaffProfileId(),
                t.getDayOfWeek(),
                t.getStartTime(),
                t.getEndTime(),
                t.getSlotMinutes(),
                t.isActive(),
                t.getNote()
        );
    }
}
