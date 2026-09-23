package com.thoughtsandopinions.apigateway.utils;

import com.thoughtsandopinions.apigateway.dto.api.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class DtoMapper {

    public static OffsetDateTime toOffsetDateTime(com.google.protobuf.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()).atOffset(ZoneOffset.UTC);
    }

    public static UserDTO map(identity.UserDetails user) {
        return new UserDTO(
                user.getUserId(),
                user.getName(),
                user.getUsername(),
                !user.getProfilePic().isEmpty() ? user.getProfilePic() : null
        );
    }

    public static UserDTO map(thoughts.Thoughts.Users user) {
        return new UserDTO(
                user.getUserId(),
                user.getName(),
                user.getUsername(),
                user.hasProfilePicUrl() ? user.getProfilePicUrl() : null
        );
    }

    public static ProfileDTO map(identity.ProfileData profile) {
        return new ProfileDTO(
                profile.getUserId(),
                profile.getName(),
                profile.getUsername(),
                profile.getBio(),
                !profile.getProfilePic().isEmpty() ? profile.getProfilePic() : null,
                profile.getFollowersCount(),
                profile.getFollowingCount(),
                toOffsetDateTime(profile.getJoinedAt())
        );
    }

    public static ThoughtDetailsDTO map(thoughts.Thoughts.GetThoughtResponse response) {
        return new ThoughtDetailsDTO(
                response.getThoughtId(),
                response.hasUser() ? map(response.getUser()) : null,
                response.getContent(),
                response.getLikesCount(),
                response.getOpinionsCount(),
                response.getRepostsCount(),
                response.getParentThoughtId() != 0 ? response.getParentThoughtId() : null,
                toOffsetDateTime(response.getCreatedAt()),
                response.getIsLiked(),
                response.getIsReposted()
        );
    }

    public static ThoughtDetailsDTO map(thoughts.Thoughts.ThoughtDetails details) {
        return new ThoughtDetailsDTO(
                details.getThoughtId(),
                null, // No user embedded in ThoughtDetails
                details.getContent(),
                details.getLikesCount(),
                details.getOpinionsCount(),
                details.getRepostsCount(),
                details.getParentThoughtId() != 0 ? details.getParentThoughtId() : null,
                toOffsetDateTime(details.getCreatedAt()),
                details.getIsLiked(),
                details.getIsReposted()
        );
    }

    public static CreateThoughtResponseDTO map(thoughts.Thoughts.CreateResponse response) {
        return new CreateThoughtResponseDTO(
                response.getThoughtId(),
                toOffsetDateTime(response.getCreatedAt())
        );
    }

    public static ThoughtsFeedDTO map(timeline.Timeline.GetFeedResponse response) {
        return new ThoughtsFeedDTO(
                response.getThoughtsListList().stream().map(DtoMapper::map).toList(),
                !response.getNextCursor().isEmpty() ? response.getNextCursor() : null
        );
    }
}
