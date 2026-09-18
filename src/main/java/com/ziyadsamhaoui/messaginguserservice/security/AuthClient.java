package com.ziyadsamhaoui.messaginguserservice.security;

import com.ziyadsamhaoui.messaginguserservice.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestTemplate restTemplate;

    @Value("${badrlink.security.auth-service.base-url}")
    private String authServiceBaseUrl;

    @Value("${badrlink.security.internal.hmac-secret}")
    private String internalSecret;

    public void changeRole(UUID userId, UserType role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Token", internalSecret);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("role", role.name()), headers);
        restTemplate.exchange(
                authServiceBaseUrl + "/internal/credentials/" + userId + "/role",
                HttpMethod.PATCH,
                entity,
                Void.class
        );
    }
}
