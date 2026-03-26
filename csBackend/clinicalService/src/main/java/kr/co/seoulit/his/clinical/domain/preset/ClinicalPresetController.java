package kr.co.seoulit.his.clinical.domain.preset;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clinical/presets")
public class ClinicalPresetController {
    @GetMapping("/order-options")
    public Map<String, Object> orderOptions() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("lab", List.of("혈액검사", "소변검사", "내시경검사", "검사없음"));
        out.put("radiology", List.of("MRI", "CT", "초음파검사", "검사없음"));
        out.put("finalOrderTypes", List.of("약제", "수술", "입원", "이상소견없음"));
        out.put("medications", List.of("아세트아미노펜", "이부프로펜", "나프록센", "판토프라졸", "알마게이트", "돔페리돈", "세티리진", "암로디핀", "로수바스타틴", "에스시탈로프람"));
        return out;
    }
}
