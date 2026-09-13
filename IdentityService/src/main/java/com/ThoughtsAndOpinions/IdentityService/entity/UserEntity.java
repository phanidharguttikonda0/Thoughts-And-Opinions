package com.ThoughtsAndOpinions.IdentityService.entity;


import com.ThoughtsAndOpinions.IdentityService.utils.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;


import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
@Getter
@Setter
@Entity
@Table(name = "users", schema = "identity_db")
public class UserEntity {

    @Id
    @SnowflakeId
    @Column(name = "id", nullable = false, updatable = false)
    private Long id; // Manually assigned (e.g., Snowflake ID)

    @Column(unique = true, nullable = false, length = 60)
    private String username;

    @Column(unique = true, nullable = false, length = 256)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "profile_pic_url", columnDefinition = "TEXT")
    private String profilePicUrl;

    @Column(length = 250)
    private String bio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // --- Relationships ---

    // People this user is following
    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FollowerEntity> following = new HashSet<>();

    // People who follow this user
    @Getter
    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FollowerEntity> followers = new HashSet<>();

    public UserEntity() {}

    public UserEntity(String username, String email, String passwordHash) {
        this.username = username ;
        this.email = email;
        this.passwordHash = passwordHash ;
    }

    // Helper methods to keep relationships in sync
    public void addFollowing(UserEntity userToFollow) {
        FollowerEntity followRelation = new FollowerEntity(this, userToFollow);
        this.following.add(followRelation);
        userToFollow.getFollowers().add(followRelation);
    }

    public void removeFollowing(UserEntity userToUnfollow) {
        FollowerEntity followRelation = new FollowerEntity(this, userToUnfollow);
        this.following.remove(followRelation);
        userToUnfollow.getFollowers().remove(followRelation);
    }

    // Getters and Setters omitted for brevity

}