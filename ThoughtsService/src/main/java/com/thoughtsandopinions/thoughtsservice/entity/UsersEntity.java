package com.thoughtsandopinions.thoughtsservice.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "users_cache", schema = "thoughts_db")
public class UsersEntity implements Serializable {

    @Id
    private long id; // we are going set id, passed by the identity service

    @Column(length = 60, nullable = false, unique = true)
    private String username;

    @Column(length = 60, nullable = false)
    private String name;

    @Column(name = "profile_pic_url", columnDefinition = "TEXT")
    private String profilePicUrl ;

    // Cascade.ALL means remove from collection = delete from DB. when we remove from colleciton
    // it get's removed from db as well.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ThoughtsEntity> thoughts = new HashSet<>() ;


    public UsersEntity(long id, String username, String name, Optional<String> profilePicUrl) {
        this.id = id ;
        this.username = username ;
        this.name = name ;
        profilePicUrl.ifPresent(s -> this.profilePicUrl = s);
    }

    public void addThought(ThoughtsEntity thought) {
        thoughts.add(thought) ;
        thought.setUser(this); // setting the user in thought's table
        if (thought.getParentThought() != null) {
            thought.getParentThought().getOpinions().add(thought) ;
            if(thought.content != null) {
                thought.getParentThought().setOpinionsCount(thought.getParentThought().getOpinionsCount() + 1);
            }else {
                thought.getParentThought().setRepostsCount(thought.getParentThought().getRepostsCount() + 1);
            }
        }
    }

    public ThoughtsEntity removeThought(long thought_id) {
        ThoughtsEntity thoughtEntity = thoughts.stream().filter(thought -> thought.getId() == thought_id)
                .findFirst().orElseThrow() ;
        this.thoughts.remove(thoughtEntity) ;
        if(thoughtEntity.getParentThought() != null){
            thoughtEntity.getParentThought().getOpinions().remove(thoughtEntity) ; //  when parent thought present we are going to remove that from opinions list
            if (thoughtEntity.content != null) {
                thoughtEntity.getParentThought().setOpinionsCount(thoughtEntity.getParentThought().getOpinionsCount() - 1);
            }else {
                thoughtEntity.getParentThought().setRepostsCount(thoughtEntity.getParentThought().getRepostsCount() - 1);
            }
        }
        return thoughtEntity ;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // actually the equals operator checks, memory address. If both are same they are equal
        if (!(o instanceof UsersEntity that)) return false; // if object is not type of thoughtsEntity, it never be equals
        return id == that.id; // if 2 objects are created , when having 2 different memory address, we need check values inside it
        //one unique value for each is id field.
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
