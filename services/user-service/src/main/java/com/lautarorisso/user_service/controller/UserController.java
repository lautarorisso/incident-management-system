package com.lautarorisso.user_service.controller;

import com.ims.shared.dto.TeamDto;
import com.ims.shared.dto.UserDto;
import com.lautarorisso.user_service.entity.Team;
import com.lautarorisso.user_service.entity.User;
import com.lautarorisso.user_service.service.TeamService;
import com.lautarorisso.user_service.service.UserService;
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
 * <p>
 * Maps entities to the shared {@code com.ims.shared.dto} records using
 * manual mapping (no MapStruct).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User and team management APIs")
public class UserController {

    private final UserService userService;
    private final TeamService teamService;

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a user profile by their unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(toUserResponse(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users")
    @Operation(summary = "List users", description = "Returns all users or filters by team ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    public ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(name = "teamId", required = false) UUID teamId) {
        List<UserDto> users = userService.getUsers(teamId).stream()
                .map(this::toUserResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/teams/{id}")
    @Operation(summary = "Get team by ID", description = "Returns a team by its unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team found"),
            @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    })
    public ResponseEntity<TeamDto> getTeamById(@PathVariable UUID id) {
        return teamService.getTeamById(id)
                .map(team -> ResponseEntity.ok(toTeamResponse(team)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- Manual mapping helpers (no MapStruct) ---

    private UserDto toUserResponse(User user) {
        return new UserDto(
                user.getId(),
                user.getKeycloakId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.isActive(),
                user.getTeamIds(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private TeamDto toTeamResponse(Team team) {
        return new TeamDto(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCreatedAt(),
                team.getUpdatedAt());
    }
}
