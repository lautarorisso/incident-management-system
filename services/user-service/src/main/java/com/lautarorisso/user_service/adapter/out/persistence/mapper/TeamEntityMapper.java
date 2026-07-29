package com.lautarorisso.user_service.adapter.out.persistence.mapper;

import com.lautarorisso.user_service.adapter.out.persistence.entity.TeamEntity;
import com.lautarorisso.user_service.domain.model.Team;
import com.lautarorisso.user_service.domain.model.TeamId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

/**
 * MapStruct mapper between TeamEntity and domain Team.
 */
@Mapper(componentModel = "spring")
public interface TeamEntityMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToTeamId")
    Team toDomain(TeamEntity entity);

    @Mapping(target = "id", source = "id", qualifiedByName = "teamIdToUuid")
    TeamEntity toEntity(Team domain);

    @Named("uuidToTeamId")
    default TeamId uuidToTeamId(UUID uuid) {
        return uuid != null ? new TeamId(uuid) : null;
    }

    @Named("teamIdToUuid")
    default UUID teamIdToUuid(TeamId teamId) {
        return teamId != null ? teamId.getValue() : null;
    }
}
