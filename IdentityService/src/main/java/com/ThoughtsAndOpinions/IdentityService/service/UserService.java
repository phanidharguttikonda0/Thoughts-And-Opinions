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
import identity.SignUpRequest;
import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final JwtService jwt ;
    private final PasswordEncoder passwordEncoder ;
    private final FollowerRepository followerRepo ;

    public UserService(UserRepository repo, FollowerRepository followerRepo,JwtService jwt, PasswordEncoder passwordEncoder) {
        this.userRepo = repo ;
        this.jwt = jwt ;
        this.passwordEncoder = passwordEncoder ;
        this.followerRepo = followerRepo ;
    }

    @Transactional
    public AuthenticationResponse createUser(SignUpRequest userDetails) {
        // we will be having username, name, email and password
        Optional<UserEntity> usernameCheck = userRepo.findByUsername(userDetails.getUsername()) ;

        if (usernameCheck.isPresent()) {
            throw new UserExistsException(UserExistsType.USERNAME);
        }

        Optional<UserEntity> userEmailCheck = userRepo.findByEmail(userDetails.getEmail()) ;

        if (userEmailCheck.isPresent()) {
            throw new UserExistsException(UserExistsType.EMAIL);
        }

        String hashedPassword = passwordEncoder.encode(userDetails.getPassword()) ;

        UserEntity user = userRepo.save(new UserEntity(userDetails.getUsername(), userDetails.getEmail(), hashedPassword)) ;

        // creating a jwt token

        String token = jwt.genrateToken(user.getUsername(), user.getId()) ;

        return new AuthenticationResponse(user.getId(), token) ;

    }

    public AuthenticationResponse authenticateUser(String username, String password) {
        // let's hash the password
        String hashedPassword = passwordEncoder.encode(password) ;

        Optional<UserEntity> user = userRepo.findByUsername(username) ;

        if(user.isEmpty()) {
            throw new UserNotFoundException("username : "+username+" not found") ;
        }else if (user.get().getPasswordHash() != hashedPassword) {
            throw new InCorrectCredentials("Invalid Credentials Passed") ;
        }

        String token = jwt.genrateToken(username, user.get().getId()) ;

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
            Optional<UserEntity> followingUser = userRepo.findById(followingId) ;
            if (followingUser.isPresent()) {

                Set<FollowerEntity> followings = user.get().getFollowing() ;

                boolean alreadyExists = followings.stream().anyMatch(
                        f -> f.getFollowing().getId() == followingId
                ) ;

                if (alreadyExists) {
                    throw new AlreadyFollowingException("Already Following") ;
                }else {
                    Set<FollowerEntity> followers = followingUser.get().getFollowers() ;
                    FollowerEntity followerEntity = new FollowerEntity(user.get(), followingUser.get()) ;
                    followers.add(followerEntity);
                    followings.add(followerEntity) ;

                    followerRepo.save(followerEntity) ;
                }

            }else{
                throw new UserNotFoundException("following_user_id : "+followingId+" doesn't exists") ;
            }
        }else{
            throw new UserNotFoundException("user_id : "+userId +" not found") ;
        }
    }

    public void unfollowUser(long userId, long unfollowId) {

        Optional<UserEntity> user = userRepo.findById(userId) ;

        if (user.isPresent()) {
            Optional<UserEntity> unfollowUser = userRepo.findById(unfollowId) ;

            if (unfollowUser.isPresent()) {

                FollowerId followerId = new FollowerId(userId, unfollowId) ;

                Set<FollowerEntity> followers = unfollowUser.get().getFollowers() ;
                Set<FollowerEntity> following = user.get().getFollowing();

                FollowerEntity followRelation = followers.stream().filter(
                        f -> f.getFollower().getId() == userId
                ).findFirst().orElseThrow(() -> new NotFollowingException("No Follow Entity in Database")) ;

                followers.remove(followRelation) ;
                following.remove(followRelation) ;

                followerRepo.deleteById(followerId);

            }else {
                throw new UserNotFoundException("unfollowId : "+unfollowId+" doesn't exists") ;
            }

        }else{
            throw new UserNotFoundException("user_id : "+userId +" not found") ;
        }
    }


    public Profile getUserProfile(long  userId) {

        Optional<UserEntity> user = userRepo.findById(userId) ;

        if (user.isEmpty()) {
            throw new UserNotFoundException("userId : "+userId+" not found") ;
        }

        // we need to return these
        return new Profile(userId, user.get().getUsername(),
                user.get().getName(), user.get().getBio(), user.get().getProfilePicUrl(),
                user.get().getFollowers().size(), user.get().getFollowing().size(), user.get().getCreatedAt()) ;


    }

    public ArrayList<ProfileDetails> getSearchedProfile(String prefix) {
        // we need to get the  profiles of 5 users by applying LikeWise on database query

        List<ProfileDetails> profileDetailsUsers = userRepo.findTop5UsersByUsernameStartingWith(
                prefix, PageRequest.of(0,5)
        ) ;


        return (ArrayList<ProfileDetails>) profileDetailsUsers;
    }
}
