package kr.co.seoulit.his.admin.domain.master.patient;

import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientResolveRequest;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientResolveResponse;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientResponse;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/master/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService service;

    /**
     * 예) GET /master/patients?ids=1,2,3
     */
    @GetMapping
    public ResponseEntity<List<PatientResponse>> listByIds(@RequestParam String ids) {
        List<Long> list = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return ResponseEntity.ok(service.listByIds(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<PatientResponse> upsert(@Valid @RequestBody PatientUpsertRequest req) {
        return ResponseEntity.ok(service.upsert(req));
    }

    /**
     * [B-2] 이름+전화로 환자 조회 후 없으면 생성.
     * 예) POST /master/patients/resolve  { name, gender, phone, rrnFront, rrnBack }
     * 프론트엔드의 patientId = Date.now() 임시 ID 발명 구조를 대체한다.
     */
    @PostMapping("/resolve")
    public ResponseEntity<PatientResolveResponse> resolve(@Valid @RequestBody PatientResolveRequest req) {
        return ResponseEntity.ok(service.resolveByNamePhone(req));
    }
}
