package com.thoughtsandopinions.thoughtsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mentions", schema = "thoughts_db")
public class MentionsEntity implements Serializable {

    @EmbeddedId
    private MentionsId id ;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private UsersEntity user ;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("thoughtId")
    @JoinColumn(name = "thought_id")
    private ThoughtsEntity thought ;

    @Column(name = "created_at")
    private OffsetDateTime createdAt ;

    public MentionsEntity(UsersEntity user, ThoughtsEntity thought) {
        this.user = user ;
        this.thought = thought ;
        this.id = new MentionsId(user.getId(), thought.getId()) ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MentionsEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
