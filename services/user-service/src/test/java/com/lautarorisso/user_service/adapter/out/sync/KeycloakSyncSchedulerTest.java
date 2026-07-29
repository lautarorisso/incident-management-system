package com.lautarorisso.user_service.adapter.out.sync;

import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakGroupResponse;
import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakUserResponse;
import com.lautarorisso.user_service.adapter.out.keycloak.mapper.KeycloakGroupMapper;
import com.lautarorisso.user_service.adapter.out.keycloak.mapper.KeycloakUserMapper;
import com.lautarorisso.user_service.domain.port.out.KeycloakAdminClient;
import com.lautarorisso.user_service.domain.port.out.TeamRepository;
import com.lautarorisso.user_service.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakSyncSchedulerTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private KeycloakUserMapper userMapper;

    @Mock
    private KeycloakGroupMapper groupMapper;

    @InjectMocks
    private KeycloakSyncScheduler scheduler;

    @Test
    void syncUsersSavesMappedUsers() {
        KeycloakUserResponse kcUser = KeycloakUserResponse.builder()
                .id(UUID.randomUUID().toString())
                .username("jdoe")
                .email("john@example.com")
                .enabled(true)
                .build();

        when(keycloakAdminClient.fetchUsers()).thenReturn(List.of(kcUser));
        when(userRepository.existsByKeycloakId(any())).thenReturn(false);

        scheduler.syncUsers();

        verify(userMapper).toDomain(kcUser);
    }

    @Test
    void syncUsersSkipsExistingUsers() {
        KeycloakUserResponse kcUser = KeycloakUserResponse.builder()
                .id(UUID.randomUUID().toString())
                .username("jdoe")
                .email("john@example.com")
                .enabled(true)
                .build();

        when(keycloakAdminClient.fetchUsers()).thenReturn(List.of(kcUser));
        when(userRepository.existsByKeycloakId(any())).thenReturn(true);

        scheduler.syncUsers();

        verify(userMapper, never()).toDomain(any());
    }

    @Test
    void syncUsersHandlesEmptyList() {
        when(keycloakAdminClient.fetchUsers()).thenReturn(List.of());

        scheduler.syncUsers();

        verify(userRepository, never()).save(any());
    }

    @Test
    void syncGroupsSavesMappedGroups() {
        KeycloakGroupResponse kcGroup = KeycloakGroupResponse.builder()
                .id(UUID.randomUUID().toString())
                .name("SRE")
                .build();

        when(keycloakAdminClient.fetchGroups()).thenReturn(List.of(kcGroup));
        when(teamRepository.existsByName(any())).thenReturn(false);

        scheduler.syncGroups();

        verify(groupMapper).toDomain(kcGroup);
    }

    @Test
    void syncGroupsSkipsExistingGroups() {
        KeycloakGroupResponse kcGroup = KeycloakGroupResponse.builder()
                .id(UUID.randomUUID().toString())
                .name("SRE")
                .build();

        when(keycloakAdminClient.fetchGroups()).thenReturn(List.of(kcGroup));
        when(teamRepository.existsByName(any())).thenReturn(true);

        scheduler.syncGroups();

        verify(groupMapper, never()).toDomain(any());
    }

    @Test
    void syncGroupsHandlesEmptyList() {
        when(keycloakAdminClient.fetchGroups()).thenReturn(List.of());

        scheduler.syncGroups();

        verify(teamRepository, never()).save(any());
    }
}
