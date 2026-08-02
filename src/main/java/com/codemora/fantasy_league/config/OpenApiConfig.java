package com.codemora.fantasy_league.config;

import java.util.Arrays;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Error responses are added to every operation here, in one place, rather
 * than repeated as @ApiResponse annotations on each of the ~40 endpoints --
 * they all come from the single GlobalExceptionHandler, so which status codes
 * are possible is a property of the request shape (has a body? has a path
 * variable? is it a write? is it @PreAuthorize-guarded?), not something each
 * controller author should have to restate and keep in sync by hand.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";
    private static final String VALIDATION_PROBLEM_DETAIL_SCHEMA = "ValidationProblemDetail";

    @Bean
    public OpenAPI fantasyLeagueOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fantasy League API")
                        .description("Admins run club leagues, seasons, and fixtures; users draft simulated "
                                + "players onto a fantasy squad and score points based on those players' "
                                + "simulated match performances.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token returned from POST /api/v1/auth/login or /refresh"))
                        .addSchemas(PROBLEM_DETAIL_SCHEMA, problemDetailSchema())
                        .addSchemas(VALIDATION_PROBLEM_DETAIL_SCHEMA, validationProblemDetailSchema()));
    }

    /**
     * Every response GlobalExceptionHandler can produce, attached wherever the
     * request shape makes it reachable:
     * <ul>
     *   <li>500 -- the catch-all handler, so every operation gets it.
     *   <li>401 -- every endpoint requiring authentication, i.e. every operation
     *       except the ones with an explicit empty @SecurityRequirements (AuthController).
     *   <li>403 -- only @PreAuthorize-guarded (ADMIN-only) endpoints.
     *   <li>400 -- only endpoints with a validated request body.
     *   <li>404 -- only endpoints with a path variable, since that's what a
     *       NotFoundException in this codebase is always keyed on.
     *   <li>409 -- write operations (POST/PUT/PATCH/DELETE), where domain
     *       validation in the service layer can throw ConflictException.
     * </ul>
     */
    @Bean
    public OperationCustomizer errorResponseCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            ApiResponses responses = operation.getResponses();

            responses.addApiResponse("500", problemResponse("Unexpected server error", PROBLEM_DETAIL_SCHEMA));

            boolean isPublic = operation.getSecurity() != null && operation.getSecurity().isEmpty();
            if (!isPublic) {
                responses.addApiResponse("401", problemResponse("Authentication required", PROBLEM_DETAIL_SCHEMA));
            }
            if (handlerMethod.getMethodAnnotation(PreAuthorize.class) != null) {
                responses.addApiResponse("403", problemResponse("You don't have permission to do that", PROBLEM_DETAIL_SCHEMA));
            }
            if (operation.getRequestBody() != null) {
                responses.addApiResponse("400", problemResponse("Validation failed", VALIDATION_PROBLEM_DETAIL_SCHEMA));
            }
            boolean hasPathVariable = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.hasParameterAnnotation(PathVariable.class));
            if (hasPathVariable) {
                responses.addApiResponse("404", problemResponse("The requested resource doesn't exist", PROBLEM_DETAIL_SCHEMA));
            }
            boolean isWrite = handlerMethod.getMethodAnnotation(PostMapping.class) != null
                    || handlerMethod.getMethodAnnotation(PutMapping.class) != null
                    || handlerMethod.getMethodAnnotation(PatchMapping.class) != null
                    || handlerMethod.getMethodAnnotation(DeleteMapping.class) != null;
            if (isWrite) {
                responses.addApiResponse("409", problemResponse("The request conflicts with the current state", PROBLEM_DETAIL_SCHEMA));
            }
            return operation;
        };
    }

    /**
     * Swagger UI shows a group dropdown once more than one GroupedOpenApi bean
     * exists. Every @PreAuthorize in this codebase is hasRole('ADMIN') -- there's
     * no USER-exclusive restriction anywhere -- so an ADMIN account can call
     * everything a USER account can, plus its own guarded operations. The groups
     * reflect that: "admin" is every operation reachable with an ADMIN token
     * (pathsToMatch("/**") -- GroupedOpenApi requires at least one filter, and
     * "everything" is the only one that means "unfiltered"), "user" is the
     * subset reachable without one.
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("User")
                .addOpenApiMethodFilter(method -> !method.isAnnotationPresent(PreAuthorize.class))
                .build();
    }

    private ApiResponse problemResponse(String description, String schemaName) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + schemaName))));
    }

    /** RFC 7807 fields, per ADR 0009 -- see GlobalExceptionHandler. */
    private Schema<?> problemDetailSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("type", new Schema<>().type("string").format("uri").example("about:blank"))
                .addProperty("title", new Schema<>().type("string").example("Not Found"))
                .addProperty("status", new Schema<>().type("integer").example(404))
                .addProperty("detail", new Schema<>().type("string").example("No season with id 99"))
                .addProperty("instance", new Schema<>().type("string").format("uri"));
    }

    /** Same shape as ProblemDetail, plus the field-level errors GlobalExceptionHandler adds for 400s. */
    private Schema<?> validationProblemDetailSchema() {
        Schema<?> fieldError = new Schema<>()
                .type("object")
                .addProperty("field", new Schema<>().type("string").example("playerIds"))
                .addProperty("message", new Schema<>().type("string").example("size must be between 15 and 15"));
        return problemDetailSchema()
                .addProperty("errors", new ArraySchema().items(fieldError));
    }
}
