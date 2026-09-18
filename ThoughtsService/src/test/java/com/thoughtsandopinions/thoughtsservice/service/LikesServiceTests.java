package com.thoughtsandopinions.thoughtsservice.service;

import com.thoughtsandopinions.thoughtsservice.model.UserActivitySummary;
import com.thoughtsandopinions.thoughtsservice.repository.LikesRepository;
import com.thoughtsandopinions.thoughtsservice.utils.CursorUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikesServiceTests {

    @Mock
    private LikesRepository repo;

    @InjectMocks
    private LikesService likesService;

    @Test
    void getThoughtLikes_SuccessWithCursor() {
        OffsetDateTime now = OffsetDateTime.now();
        String cursor = CursorUtils.encodeCursor(now);

        when(repo.findLikedUsers(eq(100L), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        List<UserActivitySummary> result = likesService.getThoughtLikes(100L, 10, cursor);

        assertTrue(result.isEmpty());
        verify(repo, times(1)).findLikedUsers(eq(100L), any(OffsetDateTime.class), any(Pageable.class));
    }

    @Test
    void getThoughtLikes_SuccessWithoutCursor() {
        when(repo.findLikedUsers(eq(100L), isNull(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        List<UserActivitySummary> result = likesService.getThoughtLikes(100L, 10, "");

        assertTrue(result.isEmpty());
        verify(repo, times(1)).findLikedUsers(eq(100L), isNull(), any(Pageable.class));
    }
}
