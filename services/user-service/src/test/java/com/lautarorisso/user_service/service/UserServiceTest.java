package com.lautarorisso.user_service.service;

import com.lautarorisso.user_service.entity.User;
import com.lautarorisso.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService} with a mocked repository.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldGetUserById() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("jdoe").build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        Optional<User> found = userService.getUserById(id);

        assertTrue(found.isPresent());
        assertEquals("jdoe", found.get().getUsername());
        verify(userRepository).findById(id);
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertTrue(userService.getUserById(id).isEmpty());
    }

    @Test
    void shouldGetAllUsersWhenNoTeamFilter() {
        when(userRepository.findAll()).thenReturn(List.of(
                User.builder().id(UUID.randomUUID()).username("user1").build(),
                User.builder().id(UUID.randomUUID()).username("user2").build()
        ));

        List<User> found = userService.getUsers(null);

        assertEquals(2, found.size());
        verify(userRepository).findAll();
        verify(userRepository, never()).findByTeamIdsContaining(any());
    }

    @Test
    void shouldGetUsersByTeamId() {
        UUID teamId = UUID.randomUUID();

        when(userRepository.findByTeamIdsContaining(teamId)).thenReturn(List.of(
                User.builder().id(UUID.randomUUID()).username("user1").teamIds(List.of(teamId)).build()
        ));

        List<User> found = userService.getUsers(teamId);

        assertEquals(1, found.size());
        verify(userRepository).findByTeamIdsContaining(teamId);
        verify(userRepository, never()).findAll();
    }
}
