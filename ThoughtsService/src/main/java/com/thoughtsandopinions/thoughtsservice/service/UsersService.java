package com.thoughtsandopinions.thoughtsservice.service;

import com.thoughtsandopinions.thoughtsservice.entity.MediaEntity;
import com.thoughtsandopinions.thoughtsservice.entity.ThoughtsEntity;
import com.thoughtsandopinions.thoughtsservice.entity.UsersEntity;
import com.thoughtsandopinions.thoughtsservice.exception.ThoughtNotFoundException;
import com.thoughtsandopinions.thoughtsservice.exception.UserNotFoundException;
import com.thoughtsandopinions.thoughtsservice.model.Thought;
import com.thoughtsandopinions.thoughtsservice.model.ThoughtDetails;
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

        // need to create a thought, it can be a opinion or thought or repost

        // needs to get the user
        Optional<UsersEntity> user = userRepo.findById(thoughtRequest.getUserId()) ;

        if(user.isEmpty()) {
            throw new UserNotFoundException("invalid user id") ;
        }

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

        String content = thoughtRequest.hasContent() ? thoughtRequest.getContent() : null;

        ThoughtsEntity thought = new ThoughtsEntity(user.get(), content, parentThought) ;

        user.get().addThought(thought); // it will add the thought into users thought set, and via cascade,
        // it adds to the Thoughts table and ThoughtsEntity

        // but we need id, so we are going with save
        ThoughtsEntity createdThought = thoughtsRepo.save(thought) ;

        return new ThoughtsResponse(createdThought.getId(), createdThought.getCreatedAt()) ;
    }


    @Transactional
    public void deleteThought(Thoughts.DeleteRequest thoughtRequest) {

        Optional<UsersEntity> user = userRepo.findById(thoughtRequest.getUserId()) ;

        if (user.isEmpty()) {
            throw new UserNotFoundException("invalid user id") ;
        }


        // before deleting , let's get the urls of the thoughts
        ThoughtsEntity thought = thoughtsRepo.findById(thoughtRequest.getThoughtId())
                .orElseThrow(() -> new ThoughtNotFoundException("Invalid Thought Id "+thoughtRequest.getThoughtId()));

        ArrayList<String> mediaUrls = (ArrayList<String>) thought.getMedia().stream().map(MediaEntity::getMediaUrl).toList();

        user.get().removeThought(thoughtRequest.getThoughtId()) ; // this automatically removes the thought
        // from thoughts table and also likes , mentions and media attached to this thought


        // send mediaUrls to the Kafka Event, that we do it later
    }

    @Transactional
    public void likeThought(long userId, long thoughtId) {

        // need to get the thought
        Optional<ThoughtsEntity> thought = thoughtsRepo.findById(thoughtId) ;

        if(thought.isEmpty()) {
            throw new ThoughtNotFoundException("Invalid Thought Id");
        }

        Optional<UsersEntity> user = userRepo.findById(userId) ;

        if(user.isEmpty()) {
            throw new UserNotFoundException("Invalid User Id") ;
        }

        thought.get().addLike(user.get());

    }

    @Transactional
    public void storeUser(Thoughts.Users user_) {

        UsersEntity user = new UsersEntity();
        user.setId(user_.getUserId());
        user.setName(user_.getName());
        user.setUsername(user_.getUsername());
        user.setProfilePicUrl(user_.getProfilePicUrl());

        userRepo.save(user) ;
    }



}
