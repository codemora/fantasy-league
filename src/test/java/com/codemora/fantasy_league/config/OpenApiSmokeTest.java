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

    @Test
    void everyOperationDocumentsTheGlobalExceptionHandlersCatchAllResponse() throws Exception {
        // 500 is reachable from literally any endpoint (GlobalExceptionHandler's
        // Exception handler), so OpenApiConfig's customizer adds it everywhere.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].get.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['500']").exists());
    }

    @Test
    void publicAuthEndpointsAreNotDocumentedWith401ButAuthenticatedOnesAre() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['401']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].get.responses['401']").exists());
    }

    @Test
    void onlyPreAuthorizeGuardedOperationsAreDocumentedWith403() throws Exception {
        // TeamController#create is @PreAuthorize("hasRole('ADMIN')"); #findById is not.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.responses['403']").doesNotExist());
    }

    @Test
    void onlyOperationsWithAValidatedRequestBodyAreDocumentedWith400() throws Exception {
        // TeamController#create takes @Valid @RequestBody; #findById takes no body.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.responses['400']").doesNotExist());
    }

    @Test
    void onlyOperationsWithAPathVariableAreDocumentedWith404() throws Exception {
        // TeamController#search has no path variable and can't 404; #findById does.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.responses['404']").exists());
    }

    @Test
    void onlyWriteOperationsAreDocumentedWith409() throws Exception {
        // TeamController#search (GET) can't conflict; #create (POST) can (duplicate name).
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].get.responses['409']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].post.responses['409']").exists());
    }

    @Test
    void adminGroupIncludesBothItsExclusiveOperationsAndEveryOperationASharedUserCanCall() throws Exception {
        // An ADMIN token can call its own guarded POST /api/v1/teams *and* the
        // unguarded GET anyone can call -- the admin group carries both.
        mockMvc.perform(get("/v3/api-docs/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists());
    }

    @Test
    void userGroupExcludesEveryPreAuthorizeGuardedOperationButIncludesTheSharedRest() throws Exception {
        // /api/v1/teams has POST (ADMIN-only) and GET (anyone); the user group
        // carries only the latter, even though both hang off the same path.
        mockMvc.perform(get("/v3/api-docs/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/teams'].post").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists());
    }

    @Test
    void errorResponsesReferenceTheRegisteredProblemDetailSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail").exists())
                .andExpect(jsonPath("$.components.schemas.ValidationProblemDetail").exists())
                .andExpect(jsonPath("$.components.schemas.ValidationProblemDetail.properties.errors").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/teams'].post.responses['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ValidationProblemDetail"))
                .andExpect(jsonPath(
                        "$.paths['/api/v1/teams/{id}'].get.responses['404'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"));
    }
}
