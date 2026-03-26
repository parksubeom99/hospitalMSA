package kr.co.seoulit.his.admin.domain.master.patientalert;

import kr.co.seoulit.his.admin.domain.master.patientalert.dto.PatientAlertResponse;
import kr.co.seoulit.his.admin.domain.master.patientalert.dto.PatientAlertUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/master/patient-alerts")
@RequiredArgsConstructor
public class PatientAlertController {

    private final PatientAlertService service;

    /**
     * 예) GET /master/patient-alerts?patientIds=1,2,3
     */
    @GetMapping
    public ResponseEntity<List<PatientAlertResponse>> listActive(@RequestParam String patientIds) {
        List<Long> ids = Arrays.stream(patientIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(service.listActiveByPatientIds(ids));
    }

    @PostMapping
    public ResponseEntity<PatientAlertResponse> upsert(@Valid @RequestBody PatientAlertUpsertRequest req) {
        return ResponseEntity.ok(service.upsert(req));
    }
}
