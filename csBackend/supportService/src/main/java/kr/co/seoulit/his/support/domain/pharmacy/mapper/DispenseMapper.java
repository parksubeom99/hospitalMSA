package kr.co.seoulit.his.support.domain.pharmacy.mapper;

import kr.co.seoulit.his.support.domain.pharmacy.Dispense;
import kr.co.seoulit.his.support.domain.pharmacy.dto.DispenseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DispenseMapper {
    @Mapping(source = "dispenseText", target = "note")
    DispenseResponse toResponse(Dispense entity);
}
