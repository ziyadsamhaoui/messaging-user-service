package com.ziyadsamhaoui.messaginguserservice.controller;

import com.ziyadsamhaoui.messaginguserservice.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalBlockController {

    private final BlockService blockService;

    @GetMapping("/blocks/check")
    public ResponseEntity<BlockCheckResponse> check(
            @RequestParam("a") UUID aId,
            @RequestParam("b") UUID bId) {
        boolean blocked = blockService.isBlocked(aId, bId);
        return ResponseEntity.ok(new BlockCheckResponse(blocked));
    }

    record BlockCheckResponse(boolean blocked) {}
}
