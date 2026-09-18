package com.ziyadsamhaoui.messaginguserservice.controller;

import com.ziyadsamhaoui.messaginguserservice.DTO.UserDtos.CreateUserRequest;
import com.ziyadsamhaoui.messaginguserservice.DTO.UserDtos.LastSeenRequest;
import com.ziyadsamhaoui.messaginguserservice.DTO.UserDtos.PublicUserDto;
import com.ziyadsamhaoui.messaginguserservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PostMapping("/users")
    public ResponseEntity<PublicUserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        PublicUserDto created = userService.createInternalUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/users/{id}/last-seen")
    public ResponseEntity<Void> updateLastSeen(
            @PathVariable UUID id,
            @Valid @RequestBody LastSeenRequest request) {
        userService.updateLastSeen(id, request.seenAt());
        return ResponseEntity.noContent().build();
    }
}
