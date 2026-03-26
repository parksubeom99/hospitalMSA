package kr.co.seoulit.his.support.domain.radiology.mapper;

import kr.co.seoulit.his.support.domain.radiology.RadiologyReport;
import kr.co.seoulit.his.support.domain.radiology.dto.RadiologyReportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RadiologyReportMapper {
    @Mapping(source = "radiologyReportId", target = "reportId")
    RadiologyReportResponse toResponse(RadiologyReport entity);
}
