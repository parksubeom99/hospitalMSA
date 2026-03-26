package kr.co.seoulit.his.support.domain.lab.mapper;

import kr.co.seoulit.his.support.domain.lab.LabResult;
import kr.co.seoulit.his.support.domain.lab.dto.LabResultResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LabResultMapper {
    LabResultResponse toResponse(LabResult entity);
}
