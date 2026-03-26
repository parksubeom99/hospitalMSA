package kr.co.seoulit.his.support.domain.ui;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/support/presets")
public class UiPresetController {

    @GetMapping("/ui")
    public Map<String, Object> getUiPresets() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labTests", List.of("혈액검사", "소변검사", "내시경검사"));
        out.put("radiologyTests", List.of("MRI", "CT", "초음파검사"));
        out.put("surgeryRooms", List.of("수술실1", "수술실2", "수술실3", "수술실4", "수술실5"));
        out.put("wards", List.of("병동1", "병동2", "병동3", "병동4", "병동5", "병동6", "병동7", "병동8", "병동9", "병동10"));
        out.put("supportScopes", List.of("LAB", "RAD", "PROC", "PHARM", "INJECTION", "MED_EXEC"));
        return out;
    }
}
