package kr.co.seoulit.his.admin.domain.frontoffice.appointment;

import kr.co.seoulit.his.admin.audit.AuditClient;
import kr.co.seoulit.his.admin.global.exception.BusinessException;
import kr.co.seoulit.his.admin.global.exception.ErrorCode;
import kr.co.seoulit.his.admin.domain.frontoffice.appointment.dto.AppointmentCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.appointment.dto.AppointmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appts;
    private final AuditClient audit;

    @Transactional
    public AppointmentResponse create(AppointmentCreateRequest req) {
        Appointment a = Appointment.builder()
                .patientId(req.patientId())
                .patientName(req.patientName())
                .departmentCode(req.departmentCode())
                .doctorId(req.doctorId())
                .status("BOOKED")
                .scheduledAt(req.scheduledAt())
                .createdAt(LocalDateTime.now())
                .build();
        Appointment saved = appts.save(a);

        audit.write("APPOINTMENT_CREATED", "APPOINTMENT", String.valueOf(saved.getAppointmentId()), null,
                Map.of("patientId", String.valueOf(saved.getPatientId()), "scheduledAt", String.valueOf(saved.getScheduledAt())));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id) {
        Appointment a = appts.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Appointment not found. id=" + id));
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listByDate(LocalDate date, String status) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        List<Appointment> list = (status == null)
                ? appts.findByScheduledAtBetween(from, to)
                : appts.findByStatusAndScheduledAtBetween(status, from, to);
        return list.stream().map(this::toResponse).toList();
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getAppointmentId(),
                a.getPatientId(),
                a.getPatientName(),
                a.getDepartmentCode(),
                a.getDoctorId(),
                a.getStatus(),
                a.getScheduledAt(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
