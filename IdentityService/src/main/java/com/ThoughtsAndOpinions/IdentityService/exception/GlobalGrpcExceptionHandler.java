package com.ThoughtsAndOpinions.IdentityService.exception;

import io.grpc.Status ;
import io.grpc.StatusException;
import org.jspecify.annotations.Nullable;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler {

    @Override
    public @Nullable StatusException handleException(Throwable exception) {


        if (exception instanceof  UserExistsException) {
            return Status.ALREADY_EXISTS.withDescription(
                    ((UserExistsException) exception).getType() + " Already Exists"
            ).asException() ;
        }
        if (exception instanceof  UserNotFoundException) {
            return Status.NOT_FOUND.withDescription("user not found").asException();
        }
        if(exception instanceof InCorrectCredentials) {
            return Status.PERMISSION_DENIED.withDescription("Invalid Credentials Passed").asException();
        }

        if(exception instanceof AlreadyFollowingException) {
            return Status.ALREADY_EXISTS.withDescription("Already Following").asException() ;
        }

        if (exception instanceof org.springframework.dao.DataIntegrityViolationException) {
            // this exists , by database , if username and email are exists in database
            // because we are checking and then we are commiting , so in mean while if any
            // other guy chooses a username , this error will be executed
            return Status.ALREADY_EXISTS
                    .withDescription("Registration conflict: Username or Email is already taken.")
                    .asException();
        }

        // Fallback for any other unhandled business or internal exceptions
        return Status.INTERNAL
                .withDescription("An unexpected internal error occurred.")
                .asException();
    }
}
