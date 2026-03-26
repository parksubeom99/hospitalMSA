package kr.co.seoulit.his.support.domain.ui;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/worklist")
public class WorklistSummaryController {

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) String date) {
        LocalDate target = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("LAB", 0);
        counts.put("RAD", 0);
        counts.put("PROC", 0);
        counts.put("PHARM", 0);
        counts.put("INJECTION", 0);
        counts.put("MED_EXEC", 0);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", target.toString());
        out.put("counts", counts);
        out.put("note", "초기 프론트 연동용 요약 엔드포인트 (후속 단계에서 실제 집계 쿼리로 교체)");
        return out;
    }
}
