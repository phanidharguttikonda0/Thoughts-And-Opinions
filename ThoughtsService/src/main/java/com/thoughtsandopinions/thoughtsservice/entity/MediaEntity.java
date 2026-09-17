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
@Table(name = "media")
public class MediaEntity implements Serializable{

    @EmbeddedId
    private MediaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("thoughtId")
    @JoinColumn(name = "thought_id")
    private ThoughtsEntity thought ;



    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt ;

    public MediaEntity(ThoughtsEntity thought, String mediaUrl) {
        this.thought = thought;
        this.id = new MediaId(mediaUrl, thought.getId()) ;
    }

    public String getMediaUrl(){
        return id.getMediaUrl() ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
