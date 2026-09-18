package com.ziyadsamhaoui.messaginguserservice.controller;

import com.ziyadsamhaoui.messaginguserservice.DTO.ConnectionDto;
import com.ziyadsamhaoui.messaginguserservice.service.ConnectionService;
import com.ziyadsamhaoui.messaginguserservice.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @PostMapping("/users/{id}/connect")
    public ResponseEntity<ConnectionDto> connect(
            @CurrentUser UUID requesterId,
            @PathVariable UUID id) {
        ConnectionDto connection = connectionService.request(requesterId, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(connection);
    }

    @PostMapping("/connections/{id}/accept")
    public ResponseEntity<ConnectionDto> accept(
            @CurrentUser UUID requesterId,
            @PathVariable Long id) {
        return ResponseEntity.ok(connectionService.accept(requesterId, id));
    }

    @PostMapping("/connections/{id}/decline")
    public ResponseEntity<ConnectionDto> decline(
            @CurrentUser UUID requesterId,
            @PathVariable Long id) {
        return ResponseEntity.ok(connectionService.decline(requesterId, id));
    }

    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionDto>> listConnections(@CurrentUser UUID requesterId) {
        return ResponseEntity.ok(connectionService.listActive(requesterId));
    }

    @GetMapping("/connections/pending")
    public ResponseEntity<List<ConnectionDto>> listPending(@CurrentUser UUID requesterId) {
        return ResponseEntity.ok(connectionService.listPending(requesterId));
    }
}
