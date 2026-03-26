package kr.co.seoulit.his.clinical.domain.review;

import kr.co.seoulit.his.clinical.domain.review.dto.ReviewResultsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/results")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    /**
     * visitId 기준으로 LAB/RAD/PROC 결과를 한 번에 모아 조회 (의사용 Review 화면용)
     * 예) GET /results?visitId=10
     */
    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @GetMapping
    public ResponseEntity<ReviewResultsResponse> byVisit(@RequestParam Long visitId) {
        return ResponseEntity.ok(service.getByVisit(visitId));
    }
}
