package kr.co.seoulit.his.admin.domain.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryResponse(
        Counts counts,
        List<PatientBoardRow> patients,
        LocalDateTime generatedAt
) {
    public record Counts(
            int waiting,
            int reservation,
            int emergency
    ) {}

    public record PatientBoardRow(
            Long visitId,
            Long patientId,
            String patientName,
            String departmentCode,
            String doctorId,
            String status,
            String arrivalType,
            Integer triageLevel,
            LocalDateTime createdAt
    ) {}
}
