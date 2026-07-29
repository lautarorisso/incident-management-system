package com.lautarorisso.user_service.adapter.in.rest.controller;

import com.lautarorisso.user_service.adapter.in.rest.dto.TeamResponse;
import com.lautarorisso.user_service.adapter.in.rest.dto.UserResponse;
import com.lautarorisso.user_service.adapter.in.rest.mapper.TeamRestMapper;
import com.lautarorisso.user_service.adapter.in.rest.mapper.UserRestMapper;
import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.model.UserId;
import com.lautarorisso.user_service.domain.port.out.TeamRepository;
import com.lautarorisso.user_service.domain.port.out.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for querying users and teams.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User and team management APIs")
public class UserController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final UserRestMapper userRestMapper;
    private final TeamRestMapper teamRestMapper;

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a user profile by their unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return userRepository.findById(new UserId(id))
                .map(user -> ResponseEntity.ok(userRestMapper.toResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users")
    @Operation(summary = "List users", description = "Returns all users or filters by team ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(name = "teamId", required = false) UUID teamId) {
        List<UserResponse> users;
        if (teamId != null) {
            users = userRestMapper.toResponseList(
                    userRepository.findByTeamId(new TeamId(teamId)));
        } else {
            users = userRestMapper.toResponseList(userRepository.findAll());
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping("/teams/{id}")
    @Operation(summary = "Get team by ID", description = "Returns a team by its unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team found"),
            @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    })
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable UUID id) {
        return teamRepository.findById(new TeamId(id))
                .map(team -> ResponseEntity.ok(teamRestMapper.toResponse(team)))
                .orElse(ResponseEntity.notFound().build());
    }
}
