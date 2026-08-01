package com.codemora.fantasy_league.config;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the OpenAPI doc actually generates and is reachable unauthenticated
 * (#51) -- a full context, not a slice, since that's the only way to confirm
 * springdoc + SecurityConfig are wired together correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiSpecIsReachableWithoutAuthAndDescribesKnownEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/teams']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
    }

    @Test
    void swaggerUiIsReachableWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPathIsDocumentedWithoutASecurityRequirement() throws Exception {
        // @SecurityRequirements on AuthController overrides the global bearer-auth
        // requirement -- login/register/refresh/logout are genuinely public.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security", hasSize(0)));
    }

    @Test
    void bearerAuthIsTheDefaultSecurityRequirementForEndpointsWithoutAnOverride() throws Exception {
        // TeamController has no @SecurityRequirements override, so it relies on
        // the global requirement declared in OpenApiConfig rather than repeating
        // it on every individual operation.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }
}
