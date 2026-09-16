package com.thoughtsandopinions.thoughtsservice.entity;


import com.thoughtsandopinions.thoughtsservice.exception.DuplicatedLikeException;
import com.thoughtsandopinions.thoughtsservice.exception.InvalidThoughtException;
import com.thoughtsandopinions.thoughtsservice.exception.NoLikeRemoveException;
import com.thoughtsandopinions.thoughtsservice.utils.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "thoughts", schema = "thoughts_db")
public class ThoughtsEntity implements Serializable {

    @Id
    @SnowflakeId
    @Column(name = "id", nullable = false, updatable = false)
    private long id; // we are going to generate the Snowflake id here

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UsersEntity user;

    @Column(length = 512)
    String content ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_thought_id")
    private ThoughtsEntity parentThought ;

    @Column(name = "likes_count")
    private int likesCount ;

    @Column(name = "opinions_count")
    private int opinionsCount ;

    @Column(name = "reposts_count")
    private int repostsCount ;

    @Column(name = "created_at")
    private OffsetDateTime createdAt ;

    // making bidirectional, we are not using the cascade and removing orphan, because
    // the only way to delete thought from opinions is, from user thought , from there
    // only it should be removed.
    @OneToMany(mappedBy = "parentThought")
    private Set<ThoughtsEntity> opinions = new HashSet<>() ;

    @OneToMany(mappedBy = "thought", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LikesEntity> likedUsers = new HashSet<>() ;


    // no need of orphanRemoval, because we are not removing media. media cannot be removed.
    // orphan removal is only used, when we removed the entity from parents list , then it should
    // be removed in the database or not. depends on whether orphanRemoval is true or false.
    @OneToMany(mappedBy = "thought", cascade = CascadeType.PERSIST, orphanRemoval = true) // we only need to save media automatically to db
    private Set<MediaEntity> media =  new HashSet<>();

    public ThoughtsEntity(UsersEntity user, String content, ThoughtsEntity parentThought) {
        this.user = user ;
        if (content == null && parentThought == null) {
            // throwing an Custom Exception (InvalidThoughtException)
            throw new InvalidThoughtException("Content and parentThought cannot both be absent");
        }else{
            if(content != null) {
                this.content = content ;
            }
            if(parentThought != null) {
                this.parentThought = parentThought ;
            }
        }
    }

    public void addLike(UsersEntity user) {
        Optional<LikesEntity> likedEntity = likedUsers.stream().filter(l -> l.getUser().getId() == user.getId())
                .findFirst();
        if(likedEntity.isEmpty()) {
            LikesEntity like = new LikesEntity(this, user) ;
            likedUsers.add(like) ;
            this.likesCount += 1 ;
        }else{
            throw new DuplicatedLikeException("Already Liked the Post") ;
        }
    }

    public void removeLike(long userId) {
        Optional<LikesEntity> likedEntity = likedUsers.stream().filter(l -> l.getUser().getId() == userId)
                .findFirst();

        if (likedEntity.isPresent()) {
            likedUsers.remove(likedEntity.get()) ;
            this.likesCount -= 1 ;
        }else{
            throw new NoLikeRemoveException("No like exists, to unlike") ;
        }
    }

    public void addMedia(String mediaUrl) {
        MediaEntity mediaEntity = new MediaEntity(this, mediaUrl);
        media.add(mediaEntity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // actually the equals operator checks, memory address. If both are same they are equal
        if (!(o instanceof ThoughtsEntity that)) return false; // if object is not type of thoughtsEntity, it never be equals
        return id == that.id; // if 2 objects are created , when having 2 different memory address, we need check values inside it
        //one unique value for each is id field.
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
