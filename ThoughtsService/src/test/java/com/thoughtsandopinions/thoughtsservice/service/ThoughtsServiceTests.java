package com.thoughtsandopinions.thoughtsservice.service;

import com.thoughtsandopinions.thoughtsservice.entity.ThoughtsEntity;
import com.thoughtsandopinions.thoughtsservice.entity.UsersEntity;
import com.thoughtsandopinions.thoughtsservice.exception.ThoughtNotFoundException;
import com.thoughtsandopinions.thoughtsservice.model.Thought;
import com.thoughtsandopinions.thoughtsservice.model.ThoughtDetails;
import com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary;
import com.thoughtsandopinions.thoughtsservice.repository.ThoughtsRepository;
import com.thoughtsandopinions.thoughtsservice.utils.CursorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThoughtsServiceTests {

    @Mock
    private ThoughtsRepository thoughtsRepo;

    @InjectMocks
    private ThoughtsService thoughtsService;

    private ThoughtsEntity mockThought;
    private UsersEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new UsersEntity();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        mockThought = new ThoughtsEntity(mockUser, "Test content", null);
        mockThought.setId(100L);
    }

    @Test
    void unlikeThought_Success() {
        // Need to add a like first to remove it
        mockThought.addLike(mockUser);

        when(thoughtsRepo.findById(100L)).thenReturn(Optional.of(mockThought));

        thoughtsService.unlikeThought(1L, 100L);

        assertTrue(mockThought.getLikedUsers().isEmpty());
    }

    @Test
    void unlikeThought_ThrowsThoughtNotFoundException() {
        when(thoughtsRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ThoughtNotFoundException.class, () -> thoughtsService.unlikeThought(1L, 999L));
    }

    @Test
    void getThoughtOpinions_SuccessWithCursor() {
        OffsetDateTime now = OffsetDateTime.now();
        String cursor = CursorUtils.encodeCursor(now);

        List<ThoughtDetails> mockOpinions = new ArrayList<>();
        mockOpinions.add(new ThoughtDetails(101L, "user2", 2L, "User 2", null, "Opinion", 100L, 0, 0, 0, now));

        when(thoughtsRepo.findOpinionsByThoughtId(eq(100L), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(mockOpinions);

        List<ThoughtDetails> result = thoughtsService.getThoughtOpinions(100L, 10, cursor);

        assertEquals(1, result.size());
        verify(thoughtsRepo, times(1)).findOpinionsByThoughtId(eq(100L), any(OffsetDateTime.class), any(Pageable.class));
    }

    @Test
    void getThoughtOpinions_SuccessWithoutCursor() {
        when(thoughtsRepo.findOpinionsByThoughtId(eq(100L), isNull(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        List<ThoughtDetails> result = thoughtsService.getThoughtOpinions(100L, 10, null);

        assertTrue(result.isEmpty());
        verify(thoughtsRepo, times(1)).findOpinionsByThoughtId(eq(100L), isNull(), any(Pageable.class));
    }

    @Test
    void getThoughtReposts_Success() {
        when(thoughtsRepo.findRepostedUsers(eq(100L), isNull(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        List<UserActivitySummary> result = thoughtsService.getThoughtReposts(100L, 10, "");

        assertTrue(result.isEmpty());
        verify(thoughtsRepo, times(1)).findRepostedUsers(eq(100L), isNull(), any(Pageable.class));
    }

    @Test
    void getThought_Success_OriginalThought() {
        mockThought.setCreatedAt(OffsetDateTime.now());

        when(thoughtsRepo.findById(100L)).thenReturn(Optional.of(mockThought));

        ThoughtDetails result = thoughtsService.getThought(100L);

        assertNotNull(result);
        assertEquals(100L, result.getThoughtId());
        assertEquals("testuser", result.getUsername());
        assertEquals("Test content", result.getContent());
        assertNull(result.getParentThoughtId());
    }

    @Test
    void getThought_Success_WithParentThought() {
        ThoughtsEntity parent = new ThoughtsEntity();
        parent.setId(99L);
        
        ThoughtsEntity comment = new ThoughtsEntity(mockUser, "Comment", parent);
        comment.setId(101L);
        comment.setCreatedAt(OffsetDateTime.now());

        when(thoughtsRepo.findById(101L)).thenReturn(Optional.of(comment));

        ThoughtDetails result = thoughtsService.getThought(101L);

        assertNotNull(result);
        assertEquals(101L, result.getThoughtId());
        assertEquals(99L, result.getParentThoughtId());
    }

    @Test
    void getThought_ThrowsThoughtNotFoundException() {
        when(thoughtsRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ThoughtNotFoundException.class, () -> thoughtsService.getThought(999L));
    }

    @Test
    void getUserThoughtHistory_Success() {
        when(thoughtsRepo.findThoughtsOfUser(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        List<Thought> result = thoughtsService.getUserThoughtHistory(1L, 10, null);

        assertTrue(result.isEmpty());
        verify(thoughtsRepo, times(1)).findThoughtsOfUser(eq(1L), isNull(), any(Pageable.class));
    }
}
