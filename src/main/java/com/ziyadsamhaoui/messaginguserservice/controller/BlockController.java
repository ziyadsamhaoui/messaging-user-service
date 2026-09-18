package com.ziyadsamhaoui.messaginguserservice.controller;

import com.ziyadsamhaoui.messaginguserservice.service.BlockService;
import com.ziyadsamhaoui.messaginguserservice.security.CurrentUser;
import com.ziyadsamhaoui.messaginguserservice.exception.ForbiddenOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/users/{id}/block")
    public ResponseEntity<Void> block(
            @CurrentUser UUID requesterId,
            @PathVariable UUID id) {
        blockService.block(requesterId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}/block")
    public ResponseEntity<Void> unblock(
            @CurrentUser UUID requesterId,
            @PathVariable UUID id) {
        blockService.unblock(requesterId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/block")
    public ResponseEntity<List<UUID>> listBlocked(@CurrentUser UUID requesterId, @PathVariable UUID id) {
        if (!requesterId.equals(id)) {
            throw new ForbiddenOperationException("users can only list their own blocks");
        }
        return ResponseEntity.ok(blockService.listBlockedUsers(requesterId));
    }
}
