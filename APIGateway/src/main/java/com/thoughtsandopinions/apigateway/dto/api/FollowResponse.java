package com.thoughtsandopinions.apigateway.dto.api;

import identity.UserDetails;
import java.util.List;


public record FollowResponse (
        List<UserDetails> users,
        String nextCursor
){
}
