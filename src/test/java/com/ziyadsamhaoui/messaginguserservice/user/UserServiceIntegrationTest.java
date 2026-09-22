package com.ziyadsamhaoui.messaginguserservice.user;

import com.ziyadsamhaoui.messaginguserservice.model.User;
import com.ziyadsamhaoui.messaginguserservice.enums.UserType;
import com.ziyadsamhaoui.messaginguserservice.repository.UserRepository;
import com.ziyadsamhaoui.messaginguserservice.TestcontainersConfiguration;
import com.ziyadsamhaoui.messaginguserservice.security.AuthClient;
import com.ziyadsamhaoui.messaginguserservice.support.TestJwtFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UserServiceIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuthClient authClient;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();
        Mockito.reset(authClient);
        Mockito.doNothing().when(authClient).changeRole(any(), any());
    }

    private String authHeader(UUID userId, String role) {
        return "Bearer " + TestJwtFactory.token(userId, role);
    }

    private UUID seedUser(String username) {
        UUID id = UUID.randomUUID();
        userRepository.save(User.builder().id(id).username(username).type(UserType.USER).build());
        return id;
    }

    @Test
    void internalRegistrationCreatesProfileWithDefaults() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/internal/users")
                        .header("X-Internal-Token", "test-internal-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\",\"username\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.type").value("USER"));
    }

    @Test
    void internalRegistrationIsIdempotent() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"id\":\"" + id + "\",\"username\":\"alice\"}";
        mockMvc.perform(post("/internal/users").header("X-Internal-Token", "test-internal-secret")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/internal/users").header("X-Internal-Token", "test-internal-secret")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void internalRegistrationWithoutTokenIsUnauthorized() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\",\"username\":\"alice\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalRegistrationWithDuplicateUsernameIsConflict() throws Exception {
        seedUser("alice");
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/internal/users")
                        .header("X-Internal-Token", "test-internal-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\",\"username\":\"alice\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void publicEndpointWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserReturnsPublicFieldsOnly() throws Exception {
        UUID id = seedUser("bob");
        mockMvc.perform(get("/users/{id}", id).header("Authorization", authHeader(id, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.type").value("USER"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void searchByPrefixReturnsMatches() throws Exception {
        seedUser("carol");
        seedUser("carla");
        seedUser("dave");
        UUID viewer = seedUser("viewer");
        mockMvc.perform(get("/users/search").param("q", "car")
                        .header("Authorization", authHeader(viewer, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateProfileByOwnerSucceeds() throws Exception {
        UUID id = seedUser("erin");
        mockMvc.perform(patch("/users/{id}", id)
                        .header("Authorization", authHeader(id, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"erin_new\",\"description\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("erin_new"))
                .andExpect(jsonPath("$.description").value("hello"));
    }

    @Test
    void updateProfileByOtherUserIsForbidden() throws Exception {
        UUID id = seedUser("frank");
        mockMvc.perform(patch("/users/{id}", id)
                        .header("Authorization", authHeader(UUID.randomUUID(), "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"frank2\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRoleRequiresAdmin() throws Exception {
        UUID id = seedUser("grace");
        mockMvc.perform(patch("/users/{id}/role", id)
                        .header("Authorization", authHeader(id, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRoleByAdminSyncsAuthAndPersists() throws Exception {
        UUID id = seedUser("heidi");
        mockMvc.perform(patch("/users/{id}/role", id)
                        .header("Authorization", authHeader(id, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ADMIN"));
        Mockito.verify(authClient).changeRole(id, UserType.ADMIN);
    }

    @Test
    void changeRoleAuthFailureTriggersCompensation() throws Exception {
        UUID id = seedUser("ivan");
        Mockito.doThrow(new IllegalStateException("auth down"))
                .when(authClient).changeRole(any(), any());
        mockMvc.perform(patch("/users/{id}/role", id)
                        .header("Authorization", authHeader(id, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ADMIN\"}"))
                .andExpect(status().isBadGateway());
        User user = userRepository.findById(id).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(UserType.USER, user.getType());
    }
}
