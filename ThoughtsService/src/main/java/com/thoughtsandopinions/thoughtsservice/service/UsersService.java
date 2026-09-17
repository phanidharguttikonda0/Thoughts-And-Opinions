package com.thoughtsandopinions.thoughtsservice.service;

import com.thoughtsandopinions.thoughtsservice.entity.MediaEntity;
import com.thoughtsandopinions.thoughtsservice.entity.ThoughtsEntity;
import com.thoughtsandopinions.thoughtsservice.entity.UsersEntity;
import com.thoughtsandopinions.thoughtsservice.exception.ThoughtNotFoundException;
import com.thoughtsandopinions.thoughtsservice.exception.UserNotFoundException;
import com.thoughtsandopinions.thoughtsservice.model.ThoughtsResponse;
import com.thoughtsandopinions.thoughtsservice.repository.ThoughtsRepository;
import com.thoughtsandopinions.thoughtsservice.repository.UsersRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thoughts.Thoughts;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class UsersService {

    private final UsersRepository userRepo ;
    private final ThoughtsRepository thoughtsRepo ;
    private static final Logger log = LoggerFactory.getLogger(UsersService.class);

    public UsersService(UsersRepository userRepo, ThoughtsRepository thoughtsRepo) {
        this.userRepo = userRepo ;
        this.thoughtsRepo = thoughtsRepo ;
    }

    @Transactional
    public ThoughtsResponse createThought(Thoughts.CreateRequest thoughtRequest) {
        log.info("Attempting to create thought for userId: {}", thoughtRequest.getUserId());

        // need to create a thought, it can be a opinion or thought or repost

        // needs to get the user
        Optional<UsersEntity> user = userRepo.findById(thoughtRequest.getUserId()) ;

        if(user.isEmpty()) {
            log.error("Failed to create thought: User not found for userId: {}", thoughtRequest.getUserId());
            throw new UserNotFoundException("invalid user id") ;
        }

        log.info("Successfully fetched user for userId: {}", thoughtRequest.getUserId());
        // needs to get parent thought if exists
        ThoughtsEntity parentThought = null ;
        if (thoughtRequest.hasParentThoughtId()) {
            Optional<ThoughtsEntity> parentThought_ = thoughtsRepo.findById(thoughtRequest.getParentThoughtId()) ;
            if (parentThought_.isPresent()) {
                parentThought = parentThought_.get() ;
            }else{
                // need to throw Exception (custom made)
                throw new ThoughtNotFoundException("Parent Thought id was Invalid") ;
            }
        }
        log.info("let's Create a Thought Entity");
        String content = thoughtRequest.hasContent() ? thoughtRequest.getContent() : null;

        if (content == null && parentThought != null) {
            boolean alreadyReposted = thoughtsRepo.existsByUserIdAndParentThoughtIdAndContentIsNull(user.get().getId(), parentThought.getId());
            if (alreadyReposted) {
                log.warn("User {} already reposted thought {}", user.get().getId(), parentThought.getId());
                throw new com.thoughtsandopinions.thoughtsservice.exception.DuplicateRepostException("Already reposted this thought");
            }
        }

        ThoughtsEntity thought = new ThoughtsEntity(user.get(), content, parentThought) ;

        log.info("let's add thought to users set");
        user.get().addThought(thought); // it will add the thought into users thought set, and via cascade,
        // it adds to the Thoughts table and ThoughtsEntity

        log.info("let's save the thought into database using .saveAndFlush");
        // but we need id, so we are going with saveAndFlush to populate generated fields like createdAt
        ThoughtsEntity createdThought = thoughtsRepo.saveAndFlush(thought) ;
        
        log.info("Successfully created thought with id: {} for userId: {}", createdThought.getId(), thoughtRequest.getUserId());
        return new ThoughtsResponse(createdThought.getId(), createdThought.getCreatedAt()) ;
    }


    @Transactional
    public void deleteThought(Thoughts.DeleteRequest thoughtRequest) {
        log.info("Attempting to delete thoughtId: {} for userId: {}", thoughtRequest.getThoughtId(), thoughtRequest.getUserId());

        Optional<UsersEntity> user = userRepo.findById(thoughtRequest.getUserId()) ;

        if (user.isEmpty()) {
            log.error("Failed to delete thought: User not found for userId: {}", thoughtRequest.getUserId());
            throw new UserNotFoundException("invalid user id") ;
        }


        // before deleting , let's get the urls of the thoughts
        ThoughtsEntity thought = thoughtsRepo.findById(thoughtRequest.getThoughtId())
                .orElseThrow(() -> {
                    log.error("Failed to delete thought: Thought not found for thoughtId: {}", thoughtRequest.getThoughtId());
                    return new ThoughtNotFoundException("Invalid Thought Id "+thoughtRequest.getThoughtId());
                });

        List<String> mediaUrls = thought.getMedia().stream().map(MediaEntity::getMediaUrl).toList();

        user.get().removeThought(thoughtRequest.getThoughtId()) ; // this automatically removes the thought
        // from thoughts table and also likes , mentions and media attached to this thought

        log.info("Successfully deleted thoughtId: {} for userId: {}", thoughtRequest.getThoughtId(), thoughtRequest.getUserId());
        // send mediaUrls to the Kafka Event, that we do it later
    }

    @Transactional
    public void likeThought(long userId, long thoughtId) {
        log.info("Attempting to like thoughtId: {} for userId: {}", thoughtId, userId);

        // need to get the thought
        Optional<ThoughtsEntity> thought = thoughtsRepo.findById(thoughtId) ;

        if(thought.isEmpty()) {
            log.error("Failed to like thought: Thought not found for thoughtId: {}", thoughtId);
            throw new ThoughtNotFoundException("Invalid Thought Id");
        }

        Optional<UsersEntity> user = userRepo.findById(userId) ;

        if(user.isEmpty()) {
            log.error("Failed to like thought: User not found for userId: {}", userId);
            throw new UserNotFoundException("Invalid User Id") ;
        }

        thought.get().addLike(user.get());
        log.info("Successfully liked thoughtId: {} for userId: {}", thoughtId, userId);
    }

    @Transactional
    public void storeUser(Thoughts.Users user_) {
        log.info("Attempting to store user: {} (userId: {})", user_.getUsername(), user_.getUserId());
        // it actually acting as post and Put request both
        UsersEntity user = new UsersEntity();
        user.setId(user_.getUserId());
        user.setName(user_.getName());
        user.setUsername(user_.getUsername());
        if (user_.hasProfilePicUrl()) {
            user.setProfilePicUrl(user_.getProfilePicUrl());
        }

        userRepo.save(user) ;
        log.info("Successfully stored user: {} (userId: {})", user_.getUsername(), user_.getUserId());
    }



}
