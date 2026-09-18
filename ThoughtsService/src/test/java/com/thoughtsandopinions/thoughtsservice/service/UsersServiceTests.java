package com.thoughtsandopinions.thoughtsservice.service;

import com.thoughtsandopinions.thoughtsservice.entity.ThoughtsEntity;
import com.thoughtsandopinions.thoughtsservice.entity.UsersEntity;
import com.thoughtsandopinions.thoughtsservice.exception.DuplicateRepostException;
import com.thoughtsandopinions.thoughtsservice.exception.ThoughtNotFoundException;
import com.thoughtsandopinions.thoughtsservice.exception.UserNotFoundException;
import com.thoughtsandopinions.thoughtsservice.model.ThoughtsResponse;
import com.thoughtsandopinions.thoughtsservice.repository.ThoughtsRepository;
import com.thoughtsandopinions.thoughtsservice.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thoughts.Thoughts;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceTests {

    @Mock
    private UsersRepository usersRepo;

    @Mock
    private ThoughtsRepository thoughtsRepo;

    @InjectMocks
    private UsersService usersService;

    private UsersEntity mockUser;
    private ThoughtsEntity mockThought;

    @BeforeEach
    void setUp() {
        mockUser = new UsersEntity();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setThoughts(new HashSet<>());

        mockThought = new ThoughtsEntity(mockUser, "Original content", null);
        mockThought.setId(100L);
        mockThought.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void createThought_Success_OriginalThought() {
        Thoughts.CreateRequest request = Thoughts.CreateRequest.newBuilder()
                .setUserId(1L)
                .setContent("Hello World")
                .build();

        when(usersRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(thoughtsRepo.saveAndFlush(any(ThoughtsEntity.class))).thenAnswer(invocation -> {
            ThoughtsEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            entity.setCreatedAt(OffsetDateTime.now());
            return entity;
        });

        ThoughtsResponse response = usersService.createThought(request);

        assertNotNull(response);
        assertEquals(101L, response.thoughtId());
        verify(thoughtsRepo, times(1)).saveAndFlush(any(ThoughtsEntity.class));
    }

    @Test
    void createThought_Success_Repost() {
        Thoughts.CreateRequest request = Thoughts.CreateRequest.newBuilder()
                .setUserId(1L)
                .setParentThoughtId(100L)
                .build(); // No content => Repost

        when(usersRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(thoughtsRepo.findById(100L)).thenReturn(Optional.of(mockThought));
        when(thoughtsRepo.existsByUserIdAndParentThoughtIdAndContentIsNull(1L, 100L)).thenReturn(false);
        when(thoughtsRepo.saveAndFlush(any(ThoughtsEntity.class))).thenAnswer(invocation -> {
            ThoughtsEntity entity = invocation.getArgument(0);
            entity.setId(102L);
            entity.setCreatedAt(OffsetDateTime.now());
            return entity;
        });

        ThoughtsResponse response = usersService.createThought(request);

        assertNotNull(response);
        assertEquals(102L, response.thoughtId());
        verify(thoughtsRepo, times(1)).existsByUserIdAndParentThoughtIdAndContentIsNull(1L, 100L);
        verify(thoughtsRepo, times(1)).saveAndFlush(any(ThoughtsEntity.class));
    }

    @Test
    void createThought_ThrowsDuplicateRepostException() {
        Thoughts.CreateRequest request = Thoughts.CreateRequest.newBuilder()
                .setUserId(1L)
                .setParentThoughtId(100L)
                .build();

        when(usersRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(thoughtsRepo.findById(100L)).thenReturn(Optional.of(mockThought));
        // Simulate already reposted
        when(thoughtsRepo.existsByUserIdAndParentThoughtIdAndContentIsNull(1L, 100L)).thenReturn(true);

        assertThrows(DuplicateRepostException.class, () -> usersService.createThought(request));
        verify(thoughtsRepo, never()).saveAndFlush(any(ThoughtsEntity.class));
    }

    @Test
    void createThought_ThrowsUserNotFoundException() {
        Thoughts.CreateRequest request = Thoughts.CreateRequest.newBuilder()
                .setUserId(99L)
                .setContent("Hello World")
                .build();

        when(usersRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> usersService.createThought(request));
    }

    @Test
    void createThought_ThrowsThoughtNotFoundException() {
        Thoughts.CreateRequest request = Thoughts.CreateRequest.newBuilder()
                .setUserId(1L)
                .setParentThoughtId(999L)
                .setContent("My Opinion")
                .build();

        when(usersRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(thoughtsRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ThoughtNotFoundException.class, () -> usersService.createThought(request));
    }

    @Test
    void deleteThought_Success() {
        Thoughts.DeleteRequest request = Thoughts.DeleteRequest.newBuilder()
                .setUserId(1L)
                .setThoughtId(100L)
                .build();

        // Needs to have the thought in the user's set for it to be removed properly
        mockUser.addThought(mockThought);

        when(usersRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(thoughtsRepo.findById(100L)).thenReturn(Optional.of(mockThought));

        usersService.deleteThought(request);

        assertFalse(mockUser.getThoughts().contains(mockThought));
    }

    @Test
    void deleteThought_ThrowsUserNotFoundException() {
        Thoughts.DeleteRequest request = Thoughts.DeleteRequest.newBuilder()
                .setUserId(99L)
                .setThoughtId(100L)
                .build();

        when(usersRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> usersService.deleteThought(request));
    }

    @Test
    void deleteThought_ThrowsThoughtNotFoundException() {
        Thoughts.DeleteRequest request = Thoughts.DeleteRequest.newBuilder()
                .setUserId(1L)
                .setThoughtId(999L)
                .build();

        when(usersRepo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(thoughtsRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ThoughtNotFoundException.class, () -> usersService.deleteThought(request));
    }

    @Test
    void likeThought_Success() {
        when(thoughtsRepo.findById(100L)).thenReturn(Optional.of(mockThought));
        when(usersRepo.findById(1L)).thenReturn(Optional.of(mockUser));

        usersService.likeThought(1L, 100L);

        // Verify the like was added
        assertTrue(mockThought.getLikedUsers().stream().anyMatch(like -> like.getUser().getId() == 1L));
    }

    @Test
    void likeThought_ThrowsThoughtNotFoundException() {
        when(thoughtsRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ThoughtNotFoundException.class, () -> usersService.likeThought(1L, 999L));
    }

    @Test
    void likeThought_ThrowsUserNotFoundException() {
        when(thoughtsRepo.findById(100L)).thenReturn(Optional.of(mockThought));
        when(usersRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> usersService.likeThought(99L, 100L));
    }

    @Test
    void storeUser_Success() {
        Thoughts.Users userRequest = Thoughts.Users.newBuilder()
                .setUserId(2L)
                .setUsername("newuser")
                .setProfilePicUrl("http://example.com/pic.jpg")
                // Deliberately skipping setName to test null name logic
                .build();

        usersService.storeUser(userRequest);

        verify(usersRepo, times(1)).save(argThat(entity -> 
                entity.getId() == 2L && 
                entity.getUsername().equals("newuser") && 
                entity.getName().isEmpty() && 
                entity.getProfilePicUrl().equals("http://example.com/pic.jpg")
        ));
    }
}
