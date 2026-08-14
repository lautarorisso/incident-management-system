package com.lautarorisso.user_service.controller;

import com.ims.shared.dto.TeamDto;
import com.ims.shared.dto.UserDto;
import com.lautarorisso.user_service.entity.Team;
import com.lautarorisso.user_service.entity.User;
import com.lautarorisso.user_service.service.TeamService;
import com.lautarorisso.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web MVC tests for {@link UserController}.
 * <p>
 * Mocks the service layer and verifies HTTP handling and manual DTO mapping.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TeamService teamService;

    private final UUID userId = UUID.randomUUID();
    private final UUID keycloakId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @Test
    void getUserByIdReturnsUser() throws Exception {
        User user = User.builder()
                .id(userId)
                .keycloakId(keycloakId)
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .active(true)
                .build();

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/{id}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.keycloakId").value(keycloakId.toString()))
                .andExpect(jsonPath("$.username").value("jdoe"))
                .andExpect(jsonPath("$.displayName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getUserByIdReturns404WhenNotFound() throws Exception {
        when(userService.getUserById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsersByTeamIdReturnsUsers() throws Exception {
        var teamId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .keycloakId(keycloakId)
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .teamIds(List.of(teamId))
                .build();

        when(userService.getUsers(teamId)).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users")
                        .param("teamId", teamId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId.toString()))
                .andExpect(jsonPath("$[0].username").value("jdoe"))
                .andExpect(jsonPath("$[0].teamIds[0]").value(teamId.toString()));
    }

    @Test
    void getAllUsersReturnsList() throws Exception {
        User user1 = User.builder().id(UUID.randomUUID()).username("user1").build();
        User user2 = User.builder().id(UUID.randomUUID()).username("user2").build();

        when(userService.getUsers(null)).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getTeamByIdReturnsTeam() throws Exception {
        var teamId = UUID.randomUUID();
        Team team = Team.builder()
                .id(teamId)
                .name("SRE")
                .description("Site Reliability Engineering")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(teamService.getTeamById(teamId)).thenReturn(Optional.of(team));

        mockMvc.perform(get("/api/teams/{id}", teamId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(teamId.toString()))
                .andExpect(jsonPath("$.name").value("SRE"))
                .andExpect(jsonPath("$.description").value("Site Reliability Engineering"));
    }

    @Test
    void getTeamByIdReturns404WhenNotFound() throws Exception {
        when(teamService.getTeamById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/teams/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
