package kr.co.seoulit.his.admin.domain.masterpreset;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/master/presets")
public class AdminMasterPresetController {

    @GetMapping("/ui")
    public Map<String, Object> uiPreset() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("roles", List.of("DOC", "ADM", "SYS"));
        out.put("doctors", List.of(
                Map.of("name", "이순신", "dept", "내과"),
                Map.of("name", "김시민", "dept", "외과"),
                Map.of("name", "박혁거세", "dept", "영상의학과")
        ));
        out.put("adminStaff", List.of(
                Map.of("name", "원무1", "role", "ADM"),
                Map.of("name", "원무2", "role", "ADM")
        ));
        out.put("medications", List.of("아세트아미노펜", "이부프로펜", "나프록센", "판토프라졸", "알마게이트", "돔페리돈", "세티리진", "암로디핀", "로수바스타틴", "에스시탈로프람"));
        out.put("surgeryRooms", List.of(1,2,3,4,5));
        out.put("wards", List.of(1,2,3,4,5,6,7,8,9,10));
        return out;
    }
}
