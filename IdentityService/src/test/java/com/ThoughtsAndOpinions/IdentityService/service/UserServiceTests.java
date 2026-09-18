package com.ThoughtsAndOpinions.IdentityService.service;

import com.ThoughtsAndOpinions.IdentityService.entity.FollowerEntity;
import com.ThoughtsAndOpinions.IdentityService.entity.UserEntity;
import com.ThoughtsAndOpinions.IdentityService.exception.*;
import com.ThoughtsAndOpinions.IdentityService.model.AuthenticationResponse;
import com.ThoughtsAndOpinions.IdentityService.model.Profile;
import com.ThoughtsAndOpinions.IdentityService.model.ProfileDetails;
import com.ThoughtsAndOpinions.IdentityService.repository.UserRepository;
import com.ThoughtsAndOpinions.IdentityService.security.JwtService;
import com.ThoughtsAndOpinions.IdentityService.utils.CursorUtils;
import identity.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    @Mock
    private UserRepository userRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserEntity user;
    private UserEntity followTarget;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now();
        user = new UserEntity("testuser", "test@test.com", "hashed_pwd", "Test User");
        user.setId(1L);
        user.setCreatedAt(now);

        followTarget = new UserEntity("targetuser", "target@test.com", "hashed_pwd2", "Target User");
        followTarget.setId(2L);
        followTarget.setCreatedAt(now);
    }

    // ==========================================
    // createUser Tests
    // ==========================================
    @Test
    void createUser_Success() {
        SignUpRequest req = SignUpRequest.newBuilder()
                .setUsername("newuser")
                .setEmail("new@test.com")
                .setPassword("secret")
                .setName("New User")
                .build();

        when(userRepo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed_secret");

        UserEntity savedUser = new UserEntity("newuser", "new@test.com", "hashed_secret", "New User");
        savedUser.setId(3L);
        when(userRepo.save(any(UserEntity.class))).thenReturn(savedUser);
        when(jwtService.generateToken("newuser", 3L)).thenReturn("dummy_token");

        AuthenticationResponse res = userService.createUser(req);

        assertEquals(3L, res.id());
        assertEquals("dummy_token", res.token());
        verify(userRepo, times(1)).save(any(UserEntity.class));
    }

    @Test
    void createUser_UsernameExists() {
        SignUpRequest req = SignUpRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("new@test.com")
                .setPassword("secret")
                .setName("New User")
                .build();

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserExistsException ex = assertThrows(UserExistsException.class, () -> userService.createUser(req));
        assertEquals(UserExistsType.USERNAME, ex.getType());
        verify(userRepo, never()).save(any());
    }

    @Test
    void createUser_EmailExists() {
        SignUpRequest req = SignUpRequest.newBuilder()
                .setUsername("newuser")
                .setEmail("test@test.com")
                .setPassword("secret")
                .setName("New User")
                .build();

        when(userRepo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserExistsException ex = assertThrows(UserExistsException.class, () -> userService.createUser(req));
        assertEquals(UserExistsType.EMAIL, ex.getType());
        verify(userRepo, never()).save(any());
    }

    // ==========================================
    // authenticateUser Tests
    // ==========================================
    @Test
    void authenticateUser_Success() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd", "hashed_pwd")).thenReturn(true);
        when(jwtService.generateToken("testuser", 1L)).thenReturn("valid_token");

        AuthenticationResponse res = userService.authenticateUser("testuser", "pwd");

        assertEquals(1L, res.id());
        assertEquals("valid_token", res.token());
    }

    @Test
    void authenticateUser_UserNotFound() {
        when(userRepo.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.authenticateUser("missing", "pwd"));
    }

    @Test
    void authenticateUser_InvalidPassword() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed_pwd")).thenReturn(false);

        assertThrows(InCorrectCredentials.class, () -> userService.authenticateUser("testuser", "wrong"));
    }

    // ==========================================
    // updateProfile Tests (Partial Updates)
    // ==========================================
    @Test
    void updateProfile_PartialUpdates() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        // Update only username and bio
        userService.updateProfile(1L, Optional.of("updated_user"), Optional.empty(), Optional.of("New Bio"), Optional.empty());

        assertEquals("updated_user", user.getUsername());
        assertEquals("New Bio", user.getBio());
        assertEquals("Test User", user.getName()); // Unchanged
        assertNull(user.getProfilePicUrl()); // Unchanged
    }

    @Test
    void updateProfile_AllFields() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        userService.updateProfile(1L, Optional.of("u2"), Optional.of("n2"), Optional.of("b2"), Optional.of("p2"));

        assertEquals("u2", user.getUsername());
        assertEquals("n2", user.getName());
        assertEquals("b2", user.getBio());
        assertEquals("p2", user.getProfilePicUrl());
    }

    @Test
    void updateProfile_UserNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> 
                userService.updateProfile(99L, Optional.of("u"), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    // ==========================================
    // followUser & unfollowUser Tests
    // ==========================================
    @Test
    void followUser_Success() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findById(2L)).thenReturn(Optional.of(followTarget));

        userService.followUser(1L, 2L);

        assertEquals(1, user.getFollowing().size());
        assertEquals(1, followTarget.getFollowers().size());
    }

    @Test
    void followUser_AlreadyFollowing() {
        user.addFollowing(followTarget);
        
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findById(2L)).thenReturn(Optional.of(followTarget));

        assertThrows(AlreadyFollowingException.class, () -> userService.followUser(1L, 2L));
    }

    @Test
    void unfollowUser_Success() {
        user.addFollowing(followTarget);

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findById(2L)).thenReturn(Optional.of(followTarget));

        userService.unfollowUser(1L, 2L);

        assertEquals(0, user.getFollowing().size());
        assertEquals(0, followTarget.getFollowers().size());
    }

    @Test
    void unfollowUser_NotFollowing() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findById(2L)).thenReturn(Optional.of(followTarget));

        assertThrows(NotFollowingException.class, () -> userService.unfollowUser(1L, 2L));
    }

    // ==========================================
    // getUserProfile & getSearchedProfile Tests
    // ==========================================
    @Test
    void getUserProfile_Success() {
        user.addFollowing(followTarget);
        user.setBio("My Bio");
        
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        Profile profile = userService.getUserProfile(1L);

        assertEquals(1L, profile.userId());
        assertEquals("testuser", profile.username());
        assertEquals("My Bio", profile.bio());
        assertEquals(0, profile.followersCount());
        assertEquals(1, profile.followingCount());
    }

    @Test
    void getSearchedProfile_Success() {
        List<ProfileDetails> mockedProfiles = new ArrayList<>();
        mockedProfiles.add(new ProfileDetails(1L, "testuser", "Test User", null, now));
        
        when(userRepo.findTop5UsersByUsernameStartingWith(eq("test"), any(PageRequest.class)))
                .thenReturn(mockedProfiles);

        List<ProfileDetails> result = userService.getSearchedProfile("test");
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).username());
    }

    // ==========================================
    // Pagination (getFollowersList & getFollowingList)
    // ==========================================
    @Test
    void getFollowersList_FirstPage() {
        List<ProfileDetails> mockedProfiles = new ArrayList<>();
        mockedProfiles.add(new ProfileDetails(1L, "testuser", "Test User", null, now));

        when(userRepo.getFollowersList(eq(OffsetDateTime.MIN), eq(2L), any(PageRequest.class)))
                .thenReturn(mockedProfiles);

        ArrayList<ProfileDetails> result = userService.getFollowersList(2L, 10, null);
        assertEquals(1, result.size());
    }

    @Test
    void getFollowersList_WithCursor() {
        List<ProfileDetails> mockedProfiles = new ArrayList<>();
        mockedProfiles.add(new ProfileDetails(1L, "testuser", "Test User", null, now));

        String encodedCursor = CursorUtils.encodeCursor(now);
        
        when(userRepo.getFollowersList(eq(now), eq(2L), any(PageRequest.class)))
                .thenReturn(mockedProfiles);

        ArrayList<ProfileDetails> result = userService.getFollowersList(2L, 10, encodedCursor);
        assertEquals(1, result.size());
    }

    @Test
    void getFollowingList_FirstPage() {
        List<ProfileDetails> mockedProfiles = new ArrayList<>();
        mockedProfiles.add(new ProfileDetails(2L, "targetuser", "Target User", null, now));

        when(userRepo.getFollowingList(eq(OffsetDateTime.MIN), eq(1L), any(PageRequest.class)))
                .thenReturn(mockedProfiles);

        ArrayList<ProfileDetails> result = userService.getFollowingList(1L, 10, null);
        assertEquals(1, result.size());
    }

    @Test
    void getFollowingList_WithCursor() {
        List<ProfileDetails> mockedProfiles = new ArrayList<>();
        mockedProfiles.add(new ProfileDetails(2L, "targetuser", "Target User", null, now));

        String encodedCursor = CursorUtils.encodeCursor(now);
        
        when(userRepo.getFollowingList(eq(now), eq(1L), any(PageRequest.class)))
                .thenReturn(mockedProfiles);

        ArrayList<ProfileDetails> result = userService.getFollowingList(1L, 10, encodedCursor);
        assertEquals(1, result.size());
    }
}
