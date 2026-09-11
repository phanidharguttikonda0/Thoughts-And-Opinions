package com.ThoughtsAndOpinions.IdentityService.service;

import com.ThoughtsAndOpinions.IdentityService.entity.UserEntity;
import com.ThoughtsAndOpinions.IdentityService.exception.InCorrectCredentials;
import com.ThoughtsAndOpinions.IdentityService.exception.UserExistsException;
import com.ThoughtsAndOpinions.IdentityService.exception.UserExistsType;
import com.ThoughtsAndOpinions.IdentityService.exception.UserNotFoundException;
import com.ThoughtsAndOpinions.IdentityService.model.AuthenticationResponse;
import com.ThoughtsAndOpinions.IdentityService.repository.UserRepository;
import com.ThoughtsAndOpinions.IdentityService.security.JwtService;
import identity.SignUpRequest;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final JwtService jwt ;
    private final PasswordEncoder passwordEncoder ;

    public UserService(UserRepository repo, JwtService jwt, PasswordEncoder passwordEncoder) {
        this.userRepo = repo ;
        this.jwt = jwt ;
        this.passwordEncoder = passwordEncoder ;
    }

    @Transactional
    public AuthenticationResponse createUser(SignUpRequest userDetails) {
        // we will be having username, name, email and password
        Optional<UserEntity> usernameCheck = userRepo.findByUsername(userDetails.getUsername()) ;

        if (usernameCheck.isPresent()) {
            throw new UserExistsException(UserExistsType.USERNAME);
        }

        Optional<UserEntity> userEmailCheck = userRepo.findByEmail(userDetails.getEmail()) ;

        if (userEmailCheck.isPresent()) {
            throw new UserExistsException(UserExistsType.EMAIL);
        }

        String hashedPassword = passwordEncoder.encode(userDetails.getPassword()) ;

        UserEntity user = userRepo.save(new UserEntity(userDetails.getUsername(), userDetails.getEmail(), hashedPassword)) ;

        // creating a jwt token

        String token = jwt.genrateToken(user.getUsername(), user.getId()) ;

        return new AuthenticationResponse(user.getId(), token) ;

    }

    public AuthenticationResponse authenticateUser(String username, String password) {
        // let's hash the password
        String hashedPassword = passwordEncoder.encode(password) ;

        Optional<UserEntity> user = userRepo.findByUsername(username) ;

        if(user.isEmpty()) {
            throw new UserNotFoundException("username : "+username+" not found") ;
        }else if (user.get().getPasswordHash() != hashedPassword) {
            throw new InCorrectCredentials("Invalid Credentials Passed") ;
        }

        String token = jwt.genrateToken(username, user.get().getId()) ;

        return new AuthenticationResponse(user.get().getId(), token) ;
    }
}
