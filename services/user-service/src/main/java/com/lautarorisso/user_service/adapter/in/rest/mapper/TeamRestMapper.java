package com.lautarorisso.user_service.adapter.in.rest.mapper;

import com.lautarorisso.user_service.adapter.in.rest.dto.TeamResponse;
import com.lautarorisso.user_service.domain.model.Team;
import com.lautarorisso.user_service.domain.model.TeamId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

/**
 * MapStruct mapper between domain Team and REST TeamResponse.
 */
@Mapper(componentModel = "spring")
public interface TeamRestMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "teamIdToUuid")
    TeamResponse toResponse(Team team);

    List<TeamResponse> toResponseList(List<Team> teams);

    @Named("teamIdToUuid")
    default UUID teamIdToUuid(TeamId teamId) {
        return teamId != null ? teamId.getValue() : null;
    }
}
