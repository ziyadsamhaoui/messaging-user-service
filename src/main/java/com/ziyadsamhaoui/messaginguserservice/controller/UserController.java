package com.ziyadsamhaoui.messaginguserservice.controller;

import com.ziyadsamhaoui.messaginguserservice.security.CurrentUser;
import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.PublicUserDto;
import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.UpdateRoleRequest;
import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.UpdateUserRequest;
import com.ziyadsamhaoui.messaginguserservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/{id}")
    public ResponseEntity<PublicUserDto> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getPublicUser(id));
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<PublicUserDto>> search(
            @RequestParam("q") String query,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(userService.search(query, limit));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<PublicUserDto> updateProfile(
            @CurrentUser UUID requesterId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(requesterId, id, request));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PublicUserDto> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request.type()));
    }
}
