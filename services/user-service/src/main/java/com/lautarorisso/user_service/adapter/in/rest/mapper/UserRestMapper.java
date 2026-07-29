package com.lautarorisso.user_service.adapter.in.rest.mapper;

import com.lautarorisso.user_service.adapter.in.rest.dto.UserResponse;
import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

/**
 * MapStruct mapper between domain User and REST UserResponse.
 */
@Mapper(componentModel = "spring")
public interface UserRestMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "userIdToUuid")
    @Mapping(target = "teamIds", source = "teamIds", qualifiedByName = "teamIdListToUuidList")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @Named("userIdToUuid")
    default UUID userIdToUuid(com.lautarorisso.user_service.domain.model.UserId userId) {
        return userId != null ? userId.getValue() : null;
    }

    @Named("teamIdListToUuidList")
    default List<UUID> teamIdListToUuidList(List<TeamId> teamIds) {
        if (teamIds == null) return List.of();
        return teamIds.stream().map(TeamId::getValue).toList();
    }
}
