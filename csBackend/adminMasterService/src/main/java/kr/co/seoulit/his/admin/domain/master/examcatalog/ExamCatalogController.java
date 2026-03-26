package kr.co.seoulit.his.admin.domain.master.examcatalog;

import kr.co.seoulit.his.admin.domain.master.examcatalog.dto.ExamCatalogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/master/exam-catalog")
@RequiredArgsConstructor
public class ExamCatalogController {

    private final ExamCatalogService service;

    @GetMapping
    public List<ExamCatalogResponse> list(@RequestParam(required = false) String category) {
        return service.list(category);
    }
}
