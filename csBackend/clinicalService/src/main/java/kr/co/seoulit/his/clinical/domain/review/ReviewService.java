package kr.co.seoulit.his.clinical.domain.review;

import kr.co.seoulit.his.clinical.integration.support.client.SupportResultClient;
import kr.co.seoulit.his.clinical.domain.order.OrderHeader;
import kr.co.seoulit.his.clinical.domain.order.OrderRepository;
import kr.co.seoulit.his.clinical.domain.review.dto.ReviewResultsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final OrderRepository orders;
    private final SupportResultClient support;

    @Transactional(readOnly = true)
    public ReviewResultsResponse getByVisit(Long visitId) {
        List<OrderHeader> visitOrders = orders.findByVisitIdAndDeletedFalse(visitId);

        // LAB/RAD/PROC만 리뷰 대상
        List<ReviewResultsResponse.ReviewOrderResult> reviewOrders = visitOrders.stream()
                .filter(o -> {
                    String c = (o.getCategory() == null ? "" : o.getCategory().toUpperCase());
                    return c.equals("LAB") || c.equals("RAD") || c.equals("PROC");
                })
                .map(o -> {
                    String cat = o.getCategory().toUpperCase();
                    List<ReviewResultsResponse.ResultEntry> results = switch (cat) {
                        case "LAB" -> support.listLabResults(o.getOrderId()).stream()
                                .map(r -> new ReviewResultsResponse.ResultEntry("LAB", r.labResultId(), r.resultText(), r.status(), r.createdAt()))
                                .toList();
                        case "RAD" -> support.listRadiologyReports(o.getOrderId()).stream()
                                .map(r -> new ReviewResultsResponse.ResultEntry("RAD", r.reportId(), r.reportText(), r.status(), r.createdAt()))
                                .toList();
                        case "PROC" -> support.listProcedureReports(o.getOrderId()).stream()
                                .map(r -> new ReviewResultsResponse.ResultEntry("PROC", r.procedureReportId(), r.reportText(), r.status(), r.createdAt()))
                                .toList();
                        default -> List.<ReviewResultsResponse.ResultEntry>of();
                    };
                    return new ReviewResultsResponse.ReviewOrderResult(
                            o.getOrderId(),
                            o.getCategory(),
                            o.getStatus(),
                            o.getCreatedAt(),
                            results
                    );
                })
                .toList();

        return new ReviewResultsResponse(visitId, reviewOrders);
    }
}
