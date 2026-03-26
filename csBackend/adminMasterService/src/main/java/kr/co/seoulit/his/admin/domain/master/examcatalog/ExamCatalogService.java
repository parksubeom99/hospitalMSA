package kr.co.seoulit.his.admin.domain.master.examcatalog;

import kr.co.seoulit.his.admin.domain.master.examcatalog.dto.ExamCatalogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamCatalogService {

    private final ExamCatalogRepository repo;

    public List<ExamCatalogResponse> list(String category) {
        List<ExamCatalogItem> items;
        if (category == null || category.isBlank()) {
            items = repo.findByActiveOrderByCategoryAscDisplayNameKrAsc(true);
        } else {
            items = repo.findByCategoryAndActiveOrderByDisplayNameKrAsc(category, true);
        }
        return items.stream()
                .map(i -> new ExamCatalogResponse(i.getExamCatalogId(), i.getItemCode(), i.getCategory(), i.getDisplayNameKr(), i.isActive()))
                .toList();
    }
}
