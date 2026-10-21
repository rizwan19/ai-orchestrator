package com.yourbrand.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient;
import com.yourbrand.chat.integration.openrouter.OpenRouterException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"app.openrouter.api-key=test-key", "app.chat.daily-budget-usd=0.003"})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ChatApiIntegrationTest.FakeModel.class})
class ChatApiIntegrationTest {

    /** Echoes the prompt back in two chunks at a fixed cost of $0.002, or fails if asked to. */
    @TestConfiguration
    static class FakeModel {
        @Bean
        @Primary
        ChatCompletionClient fakeChatCompletionClient() {
            return (model, messages, onDelta) -> {
                String prompt = messages.getLast().content();
                if (prompt.equals("fail")) throw new OpenRouterException(402, "Insufficient credits");
                onDelta.accept("Echo: ");
                onDelta.accept(prompt);
                return new ChatCompletionClient.Completion("Echo: " + prompt, 10, 3, new BigDecimal("0.002"));
            };
        }
    }

    private static final String MODEL = "anthropic/claude-haiku-5.5";

    @Autowired
    MockMvc mvc;

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/conversations")).andExpect(status().isUnauthorized());
    }

    @Test
    void signupLoginAndDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        signup(email);

        mvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email.toUpperCase(), "correct horse")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "wrong password")))
                .andExpect(status().isUnauthorized());
        var session = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "correct horse")))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession();

        mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void rejectsMutationsWithoutCsrfToken() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueEmail(), "correct horse")))
                .andExpect(status().isForbidden());
    }

    @Test
    void streamsAReplyPersistsHistoryAndEnforcesTheDailyBudget() throws Exception {
        var session = signup(uniqueEmail());
        String conversationId = createConversation(session);

        String stream = sendMessage(session, conversationId, "Hello there");
        assertThat(stream)
                .contains("event:delta", "\"text\":\"Echo: \"", "\"text\":\"Hello there\"", "event:done")
                .contains("\"spentUsd\":0.002");

        mvc.perform(get("/api/conversations/{id}/messages", conversationId).session(session))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].role").value("user"))
                .andExpect(jsonPath("$.items[1].role").value("assistant"))
                .andExpect(jsonPath("$.items[1].content").value("Echo: Hello there"));
        mvc.perform(get("/api/conversations").session(session))
                .andExpect(jsonPath("$[0].title").value("Hello there"));

        // $0.002 + $0.002 crosses the $0.003 budget; the next request is refused before streaming.
        sendMessage(session, conversationId, "Second");
        mvc.perform(post("/api/conversations/{id}/messages", conversationId).session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(message("Third", MODEL)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void streamsProviderErrorsWithoutLeakingAccountDetails() throws Exception {
        var session = signup(uniqueEmail());
        String stream = sendMessage(session, createConversation(session), "fail");

        assertThat(stream).contains("event:error", "temporarily unavailable").doesNotContain("credits");
    }

    @Test
    void rejectsUnknownModelsAndOtherUsersConversations() throws Exception {
        var owner = signup(uniqueEmail());
        String conversationId = createConversation(owner);
        var stranger = signup(uniqueEmail());

        mvc.perform(get("/api/conversations/{id}/messages", conversationId).session(stranger))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/conversations/{id}/messages", conversationId).session(owner).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(message("Hi", "openai/gpt-6-astra")))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession signup(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "correct horse")))
                .andExpect(status().isCreated())
                .andReturn().getRequest().getSession();
    }

    private String createConversation(MockHttpSession session) throws Exception {
        String body = mvc.perform(post("/api/conversations").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String sendMessage(MockHttpSession session, String conversationId, String content) throws Exception {
        var started = mvc.perform(post("/api/conversations/{id}/messages", conversationId).session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(message(content, MODEL)))
                .andExpect(request().asyncStarted())
                // Without no-transform, compressing proxies (e.g. Next.js) buffer the whole stream.
                .andExpect(header().string("Cache-Control", containsString("no-transform")))
                .andReturn();
        return mvc.perform(asyncDispatch(started)).andReturn().getResponse().getContentAsString();
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    private static String message(String content, String model) {
        return "{\"content\":\"%s\",\"model\":\"%s\"}".formatted(content, model);
    }
}
