package kr.co.seoulit.his.clinical.domain.emr.mapper;

import kr.co.seoulit.his.clinical.domain.emr.EncounterNote;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EncounterNoteMapper {
    @Mapping(source = "noteId", target = "encounterNoteId")
    EncounterResponse toResponse(EncounterNote entity);

    List<EncounterResponse> toResponseList(List<EncounterNote> entities);
}
