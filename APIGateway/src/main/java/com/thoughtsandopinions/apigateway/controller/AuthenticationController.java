package com.thoughtsandopinions.apigateway.controller;

import com.thoughtsandopinions.apigateway.dto.api.AuthenticationResponse;
import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import com.thoughtsandopinions.apigateway.dto.api.SignInDTO;
import com.thoughtsandopinions.apigateway.dto.api.SignUpDTO;
import com.thoughtsandopinions.apigateway.gRPC.IdentityServiceGrpcHandler;
import identity.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final IdentityServiceGrpcHandler identityServiceGrpcHandler;

    @Autowired
    public AuthenticationController(IdentityServiceGrpcHandler identityServiceGrpcHandler) {
        this.identityServiceGrpcHandler = identityServiceGrpcHandler;
    }

    @PostMapping("/signup")
    public ResponseEntity<ResponseDTO<AuthenticationResponse>> signUp(@RequestBody SignUpDTO request) {
        AuthResponse response = identityServiceGrpcHandler.signUp(
                request.username(),
                request.email(),
                request.password()
        );

        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .token(response.getJwtToken())
                .build();

        ResponseDTO<AuthenticationResponse> responseDTO = ResponseDTO.<AuthenticationResponse>builder()
                .success(true)
                .message("User signed up successfully")
                .data(authResponse)
                .build();

        // need to pass the userId , name, username and profilePic Url to Thoughts Service to store it in users_cache

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PostMapping("/signin")
    public ResponseEntity<ResponseDTO<AuthenticationResponse>> signIn(@RequestBody SignInDTO request) {
        AuthResponse response = identityServiceGrpcHandler.signIn(
                request.username(), 
                request.password()
        );

        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .token(response.getJwtToken())
                .build();

        ResponseDTO<AuthenticationResponse> responseDTO = ResponseDTO.<AuthenticationResponse>builder()
                .success(true)
                .message("User signed in successfully")
                .data(authResponse)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }
}
