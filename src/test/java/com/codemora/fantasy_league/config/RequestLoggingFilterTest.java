package com.codemora.fantasy_league.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringBootTest
@AutoConfigureMockMvc
class RequestLoggingFilterTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    @Test
    void logsMethodPathAndStatusForAnUnauthenticatedRequest() throws Exception {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        } finally {
            detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("method=GET")
                .contains("path=/v3/api-docs")
                .contains("status=200")
                .contains("user_id=null"));
    }

    @Test
    void logsAuthenticatedUserIdWhenPresent() throws Exception {
        User admin = userRepository.save(User.builder()
                .username("request-logging-test-admin")
                .passwordHash("irrelevant")
                .role(Role.ADMIN)
                .build());
        String token = jwtService.generateAccessToken(admin);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            mockMvc.perform(post("/api/v1/teams")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Request Logging Test Team\"}"))
                    .andExpect(status().isCreated());
        } finally {
            detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("path=/api/v1/teams")
                .contains("user_id=" + admin.getId()));
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        logger.detachAppender(appender);
    }
}
