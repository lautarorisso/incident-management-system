package com.lautarorisso.user_service.adapter.out.keycloak;

import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakGroupResponse;
import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminClientImplTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private GroupsResource groupsResource;

    private KeycloakAdminClientImpl client;

    @BeforeEach
    void setUp() {
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        client = new KeycloakAdminClientImpl(keycloak, "master");
    }

    @Test
    void fetchUsersReturnsMappedUsers() {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(UUID.randomUUID().toString());
        rep.setUsername("jdoe");
        rep.setFirstName("John");
        rep.setLastName("Doe");
        rep.setEmail("john@example.com");
        rep.setEnabled(true);

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(List.of(rep));

        List<KeycloakUserResponse> users = client.fetchUsers();

        assertEquals(1, users.size());
        KeycloakUserResponse user = users.getFirst();
        assertEquals(rep.getId(), user.getId());
        assertEquals("jdoe", user.getUsername());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john@example.com", user.getEmail());
        assertTrue(user.isEnabled());
    }

    @Test
    void fetchUsersReturnsEmptyListWhenNoUsers() {
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(List.of());

        List<KeycloakUserResponse> users = client.fetchUsers();

        assertTrue(users.isEmpty());
    }

    @Test
    void fetchGroupsReturnsMappedGroups() {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setId(UUID.randomUUID().toString());
        rep.setName("SRE");
        rep.setPath("/SRE");

        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.groups()).thenReturn(List.of(rep));

        List<KeycloakGroupResponse> groups = client.fetchGroups();

        assertEquals(1, groups.size());
        KeycloakGroupResponse group = groups.getFirst();
        assertEquals(rep.getId(), group.getId());
        assertEquals("SRE", group.getName());
        assertEquals("/SRE", group.getPath());
    }

    @Test
    void fetchGroupsReturnsEmptyListWhenNoGroups() {
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.groups()).thenReturn(List.of());

        List<KeycloakGroupResponse> groups = client.fetchGroups();

        assertTrue(groups.isEmpty());
    }

    @Test
    void fetchUserGroupsReturnsMappedGroups() {
        UUID userId = UUID.randomUUID();
        GroupRepresentation rep = new GroupRepresentation();
        rep.setId(UUID.randomUUID().toString());
        rep.setName("SRE");
        rep.setPath("/SRE");

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.groups()).thenReturn(List.of(rep));

        List<KeycloakGroupResponse> groups = client.fetchUserGroups(userId);

        assertEquals(1, groups.size());
        assertEquals("SRE", groups.getFirst().getName());
    }

    @Test
    void fetchUserGroupsReturnsEmptyListWhenNoGroups() {
        UUID userId = UUID.randomUUID();

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.groups()).thenReturn(List.of());

        List<KeycloakGroupResponse> groups = client.fetchUserGroups(userId);

        assertTrue(groups.isEmpty());
    }
}
