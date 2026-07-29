package com.lautarorisso.user_service.domain;

import com.lautarorisso.user_service.domain.model.User;
import com.lautarorisso.user_service.domain.model.UserId;
import com.lautarorisso.user_service.domain.model.Team;
import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.port.out.UserRepository;
import com.lautarorisso.user_service.domain.port.out.TeamRepository;
import com.lautarorisso.user_service.domain.port.out.KeycloakAdminClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDomainTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    // --- UserId Tests ---

    @Test
    void userIdWrapsUuid() {
        UUID rawUuid = UUID.randomUUID();
        UserId id = new UserId(rawUuid);
        assertEquals(rawUuid, id.getValue());
    }

    @Test
    void userIdsAreEqualWhenUuidsMatch() {
        UUID rawUuid = UUID.randomUUID();
        UserId id1 = new UserId(rawUuid);
        UserId id2 = new UserId(rawUuid);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void userIdsAreNotEqualWhenUuidsDiffer() {
        UserId id1 = new UserId(UUID.randomUUID());
        UserId id2 = new UserId(UUID.randomUUID());
        assertNotEquals(id1, id2);
    }

    // --- User Model Tests ---

    @Test
    void userCanBeCreatedWithAllFields() {
        UserId id = new UserId(UUID.randomUUID());
        UUID keycloakId = UUID.randomUUID();
        TeamId teamId = new TeamId(UUID.randomUUID());
        Instant now = Instant.now();

        User user = User.builder()
                .id(id)
                .keycloakId(keycloakId)
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .active(true)
                .teamIds(List.of(teamId))
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(id, user.getId());
        assertEquals(keycloakId, user.getKeycloakId());
        assertEquals("jdoe", user.getUsername());
        assertEquals("John Doe", user.getDisplayName());
        assertEquals("john@example.com", user.getEmail());
        assertTrue(user.isActive());
        assertEquals(1, user.getTeamIds().size());
        assertEquals(teamId, user.getTeamIds().getFirst());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    void userDefaultsToActive() {
        UserId id = new UserId(UUID.randomUUID());

        User user = User.builder()
                .id(id)
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .build();

        assertTrue(user.isActive());
    }

    // --- TeamId Tests ---

    @Test
    void teamIdWrapsUuid() {
        UUID rawUuid = UUID.randomUUID();
        TeamId id = new TeamId(rawUuid);
        assertEquals(rawUuid, id.getValue());
    }

    @Test
    void teamIdsAreEqualWhenUuidsMatch() {
        UUID rawUuid = UUID.randomUUID();
        TeamId id1 = new TeamId(rawUuid);
        TeamId id2 = new TeamId(rawUuid);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void teamIdsAreNotEqualWhenUuidsDiffer() {
        TeamId id1 = new TeamId(UUID.randomUUID());
        TeamId id2 = new TeamId(UUID.randomUUID());
        assertNotEquals(id1, id2);
    }

    // --- Team Model Tests ---

    @Test
    void teamCanBeCreatedWithAllFields() {
        TeamId id = new TeamId(UUID.randomUUID());
        Instant now = Instant.now();

        Team team = Team.builder()
                .id(id)
                .name("SRE")
                .description("Site Reliability Engineering")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(id, team.getId());
        assertEquals("SRE", team.getName());
        assertEquals("Site Reliability Engineering", team.getDescription());
        assertEquals(now, team.getCreatedAt());
        assertEquals(now, team.getUpdatedAt());
    }

    // --- UserRepository Port Tests ---

    @Test
    void userRepositoryCanSaveAndFindById() {
        UserId id = new UserId(UUID.randomUUID());
        User user = User.builder()
                .id(id)
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .build();

        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(id);

        assertEquals(user, saved);
        assertTrue(found.isPresent());
        assertEquals(user, found.get());
        verify(userRepository).save(user);
        verify(userRepository).findById(id);
    }

    @Test
    void userRepositoryFindByIdReturnsEmptyWhenNotFound() {
        UserId id = new UserId(UUID.randomUUID());

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Optional<User> found = userRepository.findById(id);

        assertTrue(found.isEmpty());
        verify(userRepository).findById(id);
    }

    @Test
    void userRepositoryFindAllReturnsAllUsers() {
        UserId id1 = new UserId(UUID.randomUUID());
        UserId id2 = new UserId(UUID.randomUUID());
        List<User> allUsers = List.of(
                User.builder().id(id1).username("user1").displayName("User 1").email("u1@example.com").build(),
                User.builder().id(id2).username("user2").displayName("User 2").email("u2@example.com").build()
        );

        when(userRepository.findAll()).thenReturn(allUsers);

        List<User> found = userRepository.findAll();

        assertEquals(2, found.size());
        verify(userRepository).findAll();
    }

    @Test
    void userRepositoryCanFindByTeamId() {
        TeamId teamId = new TeamId(UUID.randomUUID());
        UserId id1 = new UserId(UUID.randomUUID());
        UserId id2 = new UserId(UUID.randomUUID());

        when(userRepository.findByTeamId(teamId)).thenReturn(List.of(
                User.builder().id(id1).username("user1").displayName("User 1").email("u1@example.com").build(),
                User.builder().id(id2).username("user2").displayName("User 2").email("u2@example.com").build()
        ));

        List<User> found = userRepository.findByTeamId(teamId);

        assertEquals(2, found.size());
        verify(userRepository).findByTeamId(teamId);
    }

    @Test
    void userRepositoryIsInterface() {
        assertTrue(UserRepository.class.isInterface());
    }

    // --- TeamRepository Port Tests ---

    @Test
    void teamRepositoryCanSaveAndFindById() {
        TeamId id = new TeamId(UUID.randomUUID());
        Team team = Team.builder()
                .id(id)
                .name("SRE")
                .description("Site Reliability Engineering")
                .build();

        when(teamRepository.save(team)).thenReturn(team);
        when(teamRepository.findById(id)).thenReturn(Optional.of(team));

        Team saved = teamRepository.save(team);
        Optional<Team> found = teamRepository.findById(id);

        assertEquals(team, saved);
        assertTrue(found.isPresent());
        assertEquals(team, found.get());
        verify(teamRepository).save(team);
        verify(teamRepository).findById(id);
    }

    @Test
    void teamRepositoryFindByIdReturnsEmptyWhenNotFound() {
        TeamId id = new TeamId(UUID.randomUUID());

        when(teamRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Team> found = teamRepository.findById(id);

        assertTrue(found.isEmpty());
        verify(teamRepository).findById(id);
    }

    @Test
    void teamRepositoryFindAllReturnsAllTeams() {
        TeamId id1 = new TeamId(UUID.randomUUID());
        TeamId id2 = new TeamId(UUID.randomUUID());
        List<Team> allTeams = List.of(
                Team.builder().id(id1).name("Team 1").description("Desc 1").build(),
                Team.builder().id(id2).name("Team 2").description("Desc 2").build()
        );

        when(teamRepository.findAll()).thenReturn(allTeams);

        List<Team> found = teamRepository.findAll();

        assertEquals(2, found.size());
        verify(teamRepository).findAll();
    }

    @Test
    void teamRepositoryIsInterface() {
        assertTrue(TeamRepository.class.isInterface());
    }

    // --- KeycloakAdminClient Port Tests ---

    @Test
    void keycloakAdminClientIsInterface() {
        assertTrue(KeycloakAdminClient.class.isInterface());
    }
}
