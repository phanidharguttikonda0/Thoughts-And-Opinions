package com.thoughtsandopinions.thoughtsservice.exception;

import io.grpc.Status ;
import io.grpc.StatusException;
import org.jspecify.annotations.Nullable;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import org.springframework.stereotype.Component;

@Component
public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler{

    @Override
    public @Nullable StatusException handleException(Throwable exception) {

        if (exception instanceof InvalidThoughtException) {
            return Status.INVALID_ARGUMENT.withDescription("Content and Parent Id both cannot be null").asException();
        }

        if(exception instanceof DuplicatedLikeException) {
            return Status.ALREADY_EXISTS.withDescription("Like Already Exists").asException() ;
        }

        if(exception instanceof  NoLikeRemoveException) {
            return Status.NOT_FOUND.withDescription("No Like to Unlike").asException();
        }


        // Fallback for any other unhandled business or internal exceptions
        return Status.INTERNAL
                .withDescription("An unexpected internal error occurred.")
                .asException();
    }
}
