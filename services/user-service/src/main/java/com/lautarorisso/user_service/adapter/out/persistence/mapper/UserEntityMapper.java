package com.lautarorisso.user_service.adapter.out.persistence.mapper;

import com.lautarorisso.user_service.adapter.out.persistence.entity.UserEntity;
import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.model.User;
import com.lautarorisso.user_service.domain.model.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

/**
 * MapStruct mapper between UserEntity and domain User.
 */
@Mapper(componentModel = "spring")
public interface UserEntityMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToUserId")
    @Mapping(target = "teamIds", source = "teamIds", qualifiedByName = "uuidListToTeamIdList")
    User toDomain(UserEntity entity);

    @Mapping(target = "id", source = "id", qualifiedByName = "userIdToUuid")
    @Mapping(target = "teamIds", source = "teamIds", qualifiedByName = "teamIdListToUuidList")
    UserEntity toEntity(User domain);

    @Named("uuidToUserId")
    default UserId uuidToUserId(UUID uuid) {
        return uuid != null ? new UserId(uuid) : null;
    }

    @Named("userIdToUuid")
    default UUID userIdToUuid(UserId userId) {
        return userId != null ? userId.getValue() : null;
    }

    @Named("uuidListToTeamIdList")
    default List<TeamId> uuidListToTeamIdList(List<UUID> uuids) {
        if (uuids == null) return List.of();
        return uuids.stream().map(TeamId::new).toList();
    }

    @Named("teamIdListToUuidList")
    default List<UUID> teamIdListToUuidList(List<TeamId> teamIds) {
        if (teamIds == null) return List.of();
        return teamIds.stream().map(TeamId::getValue).toList();
    }
}
