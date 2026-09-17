package com.thoughtsandopinions.thoughtsservice.exception;

import io.grpc.Status ;
import io.grpc.StatusException;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler{

    private static final Logger log = LoggerFactory.getLogger(GlobalGrpcExceptionHandler.class);

    @Override
    public @Nullable StatusException handleException(Throwable exception) {
        log.error("gRPC Error occurred: ", exception);

        if (exception instanceof InvalidThoughtException) {
            return Status.INVALID_ARGUMENT.withDescription("Content and Parent Id both cannot be null").asException();
        }

        if(exception instanceof DuplicatedLikeException) {
            return Status.ALREADY_EXISTS.withDescription("Like Already Exists").asException() ;
        }

        if(exception instanceof  NoLikeRemoveException) {
            return Status.NOT_FOUND.withDescription("No Like to Unlike").asException();
        }

        if (exception instanceof UserNotFoundException) {
            return Status.NOT_FOUND.withDescription("User Not Found").asException();
        }

        if(exception instanceof  ThoughtNotFoundException) {
            return Status.NOT_FOUND.withDescription("Thought Not Found").asException();
        }

        if(exception instanceof com.thoughtsandopinions.thoughtsservice.exception.DuplicateRepostException) {
            return Status.ALREADY_EXISTS.withDescription("User has already reposted this thought").asException();
        }


        if (exception instanceof org.springframework.dao.DataIntegrityViolationException) {
            return Status.INVALID_ARGUMENT
                    .withDescription("Database integrity constraint violation: " + exception.getMessage())
                    .asException();
        }

        if (exception instanceof org.springframework.dao.DataAccessException) {
            return Status.INTERNAL
                    .withDescription("Database access error: " + exception.getMessage())
                    .asException();
        }

        if (exception instanceof ConstraintViolationException) {
            return Status.INVALID_ARGUMENT
                    .withDescription("Validation error: " + exception.getMessage())
                    .asException();
        }

        // Include the exception message in the fallback for better debugging
        return Status.INTERNAL
                .withDescription("An unexpected internal error occurred: " + exception.getMessage())
                .asException();
    }
}
