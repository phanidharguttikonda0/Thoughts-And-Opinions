package com.thoughtsandopinions.apigateway.exception;

import com.thoughtsandopinions.apigateway.dto.api.ResponseDTO;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ResponseDTO<Void>> handleGrpcException(StatusRuntimeException e) {
        HttpStatus httpStatus;
        switch (e.getStatus().getCode()) {
            case NOT_FOUND:
                httpStatus = HttpStatus.NOT_FOUND;
                break;
            case ALREADY_EXISTS:
                httpStatus = HttpStatus.CONFLICT;
                break;
            case UNAUTHENTICATED:
                httpStatus = HttpStatus.UNAUTHORIZED;
                break;
            case PERMISSION_DENIED:
                httpStatus = HttpStatus.FORBIDDEN;
                break;
            case INVALID_ARGUMENT:
                httpStatus = HttpStatus.BAD_REQUEST;
                break;
            default:
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ResponseDTO<Void> response = ResponseDTO.<Void>builder()
                .success(false)
                .message(e.getStatus().getDescription() != null ? e.getStatus().getDescription() : e.getMessage())
                .build();

        return ResponseEntity.status(httpStatus).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Void>> handleGenericException(Exception e) {
        ResponseDTO<Void> response = ResponseDTO.<Void>builder()
                .success(false)
                .message("An unexpected error occurred: " + e.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
