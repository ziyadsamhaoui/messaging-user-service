package com.ziyadsamhaoui.messaginguserservice.common;

import com.ziyadsamhaoui.messaginguserservice.exception.SelfBlockedException;
import com.ziyadsamhaoui.messaginguserservice.exception.ConnectionNotFoundException;
import com.ziyadsamhaoui.messaginguserservice.exception.DuplicateConnectionException;
import com.ziyadsamhaoui.messaginguserservice.exception.ForbiddenConnectionOperationException;
import com.ziyadsamhaoui.messaginguserservice.exception.InvalidConnectionStateException;
import com.ziyadsamhaoui.messaginguserservice.exception.SelfConnectionException;
import com.ziyadsamhaoui.messaginguserservice.exception.DuplicateUsernameException;
import com.ziyadsamhaoui.messaginguserservice.exception.ForbiddenOperationException;
import com.ziyadsamhaoui.messaginguserservice.exception.RoleSyncException;
import com.ziyadsamhaoui.messaginguserservice.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConnectionNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleConnectionNotFound(ConnectionNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    ResponseEntity<Map<String, Object>> handleDuplicateUsername(DuplicateUsernameException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DuplicateConnectionException.class)
    ResponseEntity<Map<String, Object>> handleDuplicateConnection(DuplicateConnectionException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({SelfBlockedException.class, SelfConnectionException.class,
            InvalidConnectionStateException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getAllErrors().get(0).getDefaultMessage()
                : ex.getMessage();
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({ForbiddenOperationException.class, ForbiddenConnectionOperationException.class})
    ResponseEntity<Map<String, Object>> handleForbidden(Exception ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<Map<String, Object>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "access denied");
    }

    @ExceptionHandler(RoleSyncException.class)
    ResponseEntity<Map<String, Object>> handleRoleSync(RoleSyncException ex) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of(
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "message", message,
                        "timestamp", Instant.now().toString()
                ));
    }
}
