package com.thoughtsandopinions.thoughtsservice.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class MediaId implements Serializable {


    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "thought_id")
    private long thoughtId;

    public MediaId(String mediaUrl, long thoughtId) {
        this.mediaUrl = mediaUrl ;
        this.thoughtId = thoughtId ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaId that = (MediaId) o;
        return Objects.equals(mediaUrl, that.mediaUrl) &&
                Objects.equals(thoughtId, that.thoughtId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaUrl, thoughtId);
    }
}
