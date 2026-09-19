package com.thoughtsandopinions.apigateway.controller;

import com.google.protobuf.Empty;
import com.thoughtsandopinions.apigateway.dto.api.AuthenticationResponse;
import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import com.thoughtsandopinions.apigateway.dto.api.SignInDTO;
import com.thoughtsandopinions.apigateway.dto.api.SignUpDTO;
import com.thoughtsandopinions.apigateway.dto.service.UserCache;
import com.thoughtsandopinions.apigateway.gRPC.IdentityServiceGrpcHandler;
import com.thoughtsandopinions.apigateway.gRPC.ThoughtsServiceGrpcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final IdentityServiceGrpcHandler identityServiceGrpcHandler;
    private final ThoughtsServiceGrpcHandler thoughtsServiceGrpcHandler ;
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);


    public AuthenticationController(IdentityServiceGrpcHandler identityServiceGrpcHandler, ThoughtsServiceGrpcHandler thoughtsServiceGrpcHandler) {
        this.identityServiceGrpcHandler = identityServiceGrpcHandler;
        this.thoughtsServiceGrpcHandler = thoughtsServiceGrpcHandler ;
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<ResponseDTO<AuthenticationResponse>>> signUp(@RequestBody SignUpDTO request) {
        // We cannot unwrap the mono , the spring webflux unwraps it when the asynchronous operation completes
        return Mono.fromCallable(() -> identityServiceGrpcHandler.signUp(
                request.username(),
                request.email(),
                request.password()
        ))
        .subscribeOn(Schedulers.boundedElastic())
        .map(gRPCResponse -> {

            UserCache user = new UserCache(gRPCResponse.getUserId(), request.username(), null, null) ;

            // adding the user details into thoughts table user_cache in background. Even if it fails
            // that doesn't affect , because our user data will be hold in identity service only primarily.

            Mono.fromCallable(() -> thoughtsServiceGrpcHandler.userCache(user))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            empty -> {
                                log.info("Successfully added user in user cache via thoughts service");
                            },
                            error -> {
                                log.error("adding user into user cache into thoughts service failed");
                                // need to write a fallback for it.
                            }
                    ) ;



            AuthenticationResponse authResponse = AuthenticationResponse.builder()
                    .token(gRPCResponse.getJwtToken())
                    .build();

            ResponseDTO<AuthenticationResponse> responseDTO = ResponseDTO.<AuthenticationResponse>builder()
                    .success(true)
                    .message("User signed up successfully")
                    .data(authResponse)
                    .build();

            // need to pass the userId , name, username and profilePic Url to Thoughts Service to store it in users_cache
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        });
    }

    @PostMapping("/signin")
    public Mono<ResponseEntity<ResponseDTO<AuthenticationResponse>>> signIn(@RequestBody SignInDTO request) {
        return Mono.fromCallable(() -> identityServiceGrpcHandler.signIn(
                request.username(), 
                request.password()
        ))
        .subscribeOn(Schedulers.boundedElastic())
        .map(gRPCResponse -> {
            AuthenticationResponse authResponse = AuthenticationResponse.builder()
                    .token(gRPCResponse.getJwtToken())
                    .build();

            ResponseDTO<AuthenticationResponse> responseDTO = ResponseDTO.<AuthenticationResponse>builder()
                    .success(true)
                    .message("User signed in successfully")
                    .data(authResponse)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
        });
    }
}
