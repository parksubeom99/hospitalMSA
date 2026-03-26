package kr.co.seoulit.his.clinical.domain.emr;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncounterNoteRepository extends JpaRepository<EncounterNote, Long> {

    List<EncounterNote> findByVisitIdAndArchivedFalse(Long visitId);
    List<EncounterNote> findByVisitId(Long visitId);

    Optional<EncounterNote> findByNoteIdAndArchivedFalse(Long noteId);
}
