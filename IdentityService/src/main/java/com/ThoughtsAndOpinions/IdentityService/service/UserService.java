package com.ThoughtsAndOpinions.IdentityService.service;

import com.ThoughtsAndOpinions.IdentityService.entity.FollowerEntity;
import com.ThoughtsAndOpinions.IdentityService.entity.FollowerId;
import com.ThoughtsAndOpinions.IdentityService.entity.UserEntity;
import com.ThoughtsAndOpinions.IdentityService.exception.*;
import com.ThoughtsAndOpinions.IdentityService.model.AuthenticationResponse;
import com.ThoughtsAndOpinions.IdentityService.model.Profile;
import com.ThoughtsAndOpinions.IdentityService.model.ProfileDetails;
import com.ThoughtsAndOpinions.IdentityService.repository.FollowerRepository;
import com.ThoughtsAndOpinions.IdentityService.repository.UserRepository;
import com.ThoughtsAndOpinions.IdentityService.security.JwtService;
import com.ThoughtsAndOpinions.IdentityService.utils.CursorUtils;
import identity.SignUpRequest;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class UserService {

    private final UserRepository userRepo;
    private final JwtService jwt ;
    private final PasswordEncoder passwordEncoder ;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository repo, JwtService jwt, PasswordEncoder passwordEncoder) {
        this.userRepo = repo ;
        this.jwt = jwt ;
        this.passwordEncoder = passwordEncoder ;

    }

    @Transactional
    public AuthenticationResponse createUser(SignUpRequest userDetails) {
        // we will be having username, name, email and password
        Optional<UserEntity> usernameCheck = userRepo.findByUsername(userDetails.getUsername()) ;

        if (usernameCheck.isPresent()) {
            log.warn("Username already exists during sign up check : {}", userDetails.getUsername());
            throw new UserExistsException(UserExistsType.USERNAME);
        }

        Optional<UserEntity> userEmailCheck = userRepo.findByEmail(userDetails.getEmail()) ;

        if (userEmailCheck.isPresent()) {
            log.warn("Email already exists during sign in check : {}", userDetails.getEmail());
            throw new UserExistsException(UserExistsType.EMAIL);
        }


        String hashedPassword = passwordEncoder.encode(userDetails.getPassword()) ;
        log.info("password was hashed successfully");
        UserEntity user = userRepo.save(new UserEntity(userDetails.getUsername(), userDetails.getEmail(), hashedPassword, userDetails.getName())) ;
        // when we are saving the user at that time the snowflake id is injected by the hibernate.
        // creating a jwt token
        log.info("user was save and going to generate token");
        String token = jwt.generateToken(user.getUsername(), user.getId()) ;

        return new AuthenticationResponse(user.getId(), token) ;

    }

    public AuthenticationResponse authenticateUser(String username, String password) {
        Optional<UserEntity> user = userRepo.findByUsername(username) ;

        if(user.isEmpty()) {
            log.info("user not found during sign in");
            throw new UserNotFoundException("username : "+username+" not found") ;
        }
        
        if (!passwordEncoder.matches(password, user.get().getPasswordHash())) {
            log.info("Password Match Failed, Incorrect Password");
            throw new InCorrectCredentials("Invalid Credentials Passed") ;
        }

        log.info("generating the jwt token");
        String token = jwt.generateToken(username, user.get().getId()) ;

        return new AuthenticationResponse(user.get().getId(), token) ;
    }

    @Transactional
    public void updateProfile(long user_id, Optional<String> username, Optional<String> name, Optional<String> bio, Optional<String> profilePicUrl) {

        Optional<UserEntity> user = userRepo.findById(user_id) ;

        if (user.isPresent()) {
            username.ifPresent(u -> user.get().setUsername(u));

            name.ifPresent(n -> user.get().setName(n));

            bio.ifPresent(b -> user.get().setBio(b));

            // profile picture will be uploaded by API gateway and passes the profile pic url
            profilePicUrl.ifPresent(p -> user.get().setProfilePicUrl(p));
        }else {
            throw new UserNotFoundException("user_id : "+user_id +" not found") ;
        }
    }


    @Transactional
    public void followUser(long userId, long followingId) {
        Optional<UserEntity> user = userRepo.findById(userId) ;

        if(user.isPresent()) {
            log.info("Got the User entity, who sent follow request");
            Optional<UserEntity> followingUser = userRepo.findById(followingId) ;
            log.info("Got the following user entity");
            if (followingUser.isPresent()) {

                Set<FollowerEntity> followings = user.get().getFollowing() ;

                boolean alreadyExists = followings.stream().anyMatch(
                        f -> f.getFollowing().getId() == followingId
                ) ;

                if (alreadyExists) {
                    log.info("already following");
                    throw new AlreadyFollowingException("Already Following") ;
                }else {
                    log.info("Not following Already, So let's Create a Follow Relation");
                    
                    // The collections have cascade = CascadeType.ALL, so adding to them is enough.
                    // We DO NOT call followerRepo.save() because JPA will try to merge the unmanaged
                    // entity and cause a "detached entity passed to persist" error during flush.
                    user.get().addFollowing(followingUser.get());
                    
                    log.info("Stored the Follow Relation Successfully");
                }

            }else{
                throw new UserNotFoundException("following_user_id : "+followingId+" doesn't exists") ;
            }
        }else{
            throw new UserNotFoundException("user_id : "+userId +" not found") ;
        }
    }

    @Transactional
    public void unfollowUser(long userId, long unfollowId) {

        Optional<UserEntity> user = userRepo.findById(userId) ;

        if (user.isPresent()) {
            Optional<UserEntity> unfollowUser = userRepo.findById(unfollowId) ;

            if (unfollowUser.isPresent()) {
                log.info("Both users Present, let's remove from following");

                user.get().removeFollowing(unfollowUser.get());

            }else {
                throw new UserNotFoundException("unfollowId : "+unfollowId+" doesn't exists") ;
            }

        }else{
            throw new UserNotFoundException("user_id : "+userId +" not found") ;
        }
    }

    @Transactional
    public Profile getUserProfile(long  userId) {

        Optional<UserEntity> user = userRepo.findById(userId) ;

        if (user.isEmpty()) {
            log.info("user not found by id");
            throw new UserNotFoundException("userId : "+userId+" not found") ;
        }

        log.info("we found the user");
        // we need to return these
        return new Profile(userId, user.get().getUsername(),
                user.get().getName(), user.get().getBio(), user.get().getProfilePicUrl(),
                user.get().getFollowers().size(), user.get().getFollowing().size(), user.get().getCreatedAt()) ;


    }

    @Transactional
    public ArrayList<ProfileDetails> getSearchedProfile(String prefix) {
        // we need to get the  profiles of 5 users by applying LikeWise on database query

        List<ProfileDetails> profileDetailsUsers = userRepo.findTop5UsersByUsernameStartingWith(
                prefix, PageRequest.of(0,5)
        ) ;


        return (ArrayList<ProfileDetails>) profileDetailsUsers;
    }

    @Transactional
    public ArrayList<ProfileDetails> getFollowersList(long user_id, int limit, String cursor) {
        OffsetDateTime cursorTime = OffsetDateTime.MIN ;
        if (cursor != null) {
            cursorTime = CursorUtils.decodeCursor(cursor) ;
        }

        ArrayList<ProfileDetails> profileDetails = (ArrayList<ProfileDetails>) userRepo.getFollowersList(
                cursorTime, user_id, PageRequest.of(0, limit)
        );

        return profileDetails ;

    }

    @Transactional
    public ArrayList<ProfileDetails> getFollowingList(long user_id, int limit, String cursor) {

        OffsetDateTime cursorTime = OffsetDateTime.MIN ;
        if (cursor != null) {
            cursorTime = CursorUtils.decodeCursor(cursor) ;
        }

        ArrayList<ProfileDetails> profileDetails = (ArrayList<ProfileDetails>) userRepo.getFollowingList(
                cursorTime, user_id, PageRequest.of(0, limit)
        );

        return profileDetails ;

    }

}
