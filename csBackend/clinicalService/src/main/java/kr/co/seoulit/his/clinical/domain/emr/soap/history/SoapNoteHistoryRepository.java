package kr.co.seoulit.his.clinical.domain.emr.soap.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SoapNoteHistoryRepository extends JpaRepository<SoapNoteHistory, Long> {
    List<SoapNoteHistory> findByVisitIdOrderByVersionNoDesc(Long visitId);
    Optional<SoapNoteHistory> findByVisitIdAndVersionNo(Long visitId, Integer versionNo);
}
