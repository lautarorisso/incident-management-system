package com.lautarorisso.user_service.adapter.in.rest;

import com.lautarorisso.user_service.adapter.in.rest.controller.UserController;
import com.lautarorisso.user_service.adapter.in.rest.dto.TeamResponse;
import com.lautarorisso.user_service.adapter.in.rest.dto.UserResponse;
import com.lautarorisso.user_service.adapter.in.rest.mapper.TeamRestMapper;
import com.lautarorisso.user_service.adapter.in.rest.mapper.UserRestMapper;
import com.lautarorisso.user_service.domain.model.Team;
import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.model.User;
import com.lautarorisso.user_service.domain.model.UserId;
import com.lautarorisso.user_service.domain.port.out.TeamRepository;
import com.lautarorisso.user_service.domain.port.out.UserRepository;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TeamRepository teamRepository;

    @MockitoBean
    private UserRestMapper userRestMapper;

    @MockitoBean
    private TeamRestMapper teamRestMapper;

    @Test
    void getUserByIdReturnsUser() throws Exception {
        var userId = UUID.randomUUID();
        var response = UserResponse.builder()
                .id(userId)
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .active(true)
                .build();

        when(userRepository.findById(any())).thenReturn(Optional.of(User.builder().build()));
        when(userRestMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("jdoe"))
                .andExpect(jsonPath("$.displayName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getUserByIdReturns404WhenNotFound() throws Exception {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsersByTeamIdReturnsUsers() throws Exception {
        var teamId = UUID.randomUUID();
        var response = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .teamIds(List.of(teamId))
                .build();

        when(userRepository.findByTeamId(any())).thenReturn(List.of(User.builder().build()));
        when(userRestMapper.toResponseList(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users")
                        .param("teamId", teamId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("jdoe"));
    }

    @Test
    void getAllUsersReturnsList() throws Exception {
        var response1 = UserResponse.builder().id(UUID.randomUUID()).username("user1").build();
        var response2 = UserResponse.builder().id(UUID.randomUUID()).username("user2").build();

        when(userRepository.findAll()).thenReturn(List.of(User.builder().build(), User.builder().build()));
        when(userRestMapper.toResponseList(any())).thenReturn(List.of(response1, response2));

        mockMvc.perform(get("/api/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getTeamByIdReturnsTeam() throws Exception {
        var teamId = UUID.randomUUID();
        var response = TeamResponse.builder()
                .id(teamId)
                .name("SRE")
                .description("Site Reliability Engineering")
                .build();

        when(teamRepository.findById(any())).thenReturn(Optional.of(Team.builder().build()));
        when(teamRestMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(get("/api/teams/{id}", teamId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(teamId.toString()))
                .andExpect(jsonPath("$.name").value("SRE"))
                .andExpect(jsonPath("$.description").value("Site Reliability Engineering"));
    }

    @Test
    void getTeamByIdReturns404WhenNotFound() throws Exception {
        when(teamRepository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/teams/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
