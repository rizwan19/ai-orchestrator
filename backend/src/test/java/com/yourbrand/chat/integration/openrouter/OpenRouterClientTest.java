package com.yourbrand.chat.integration.openrouter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.yourbrand.chat.config.OpenRouterProperties;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient.ChatMessage;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OpenRouterClientTest {

    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authHeader = new AtomicReference<>();

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void streamsDeltasSkipsCommentsAndReadsUsage() {
        var client = clientRespondingWith(200, """
                : OPENROUTER PROCESSING

                data: {"choices":[{"delta":{"role":"assistant","content":"Hel"}}]}

                data: {"choices":[{"delta":{"content":"lo"}}]}

                data: {"choices":[{"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":12,"completion_tokens":2,"cost":0.0000042}}

                data: [DONE]

                """);
        List<String> deltas = new ArrayList<>();

        var completion = client.stream("test/model", List.of(new ChatMessage("user", "Hi")), deltas::add);

        assertThat(deltas).containsExactly("Hel", "lo");
        assertThat(completion.content()).isEqualTo("Hello");
        assertThat(completion.promptTokens()).isEqualTo(12);
        assertThat(completion.completionTokens()).isEqualTo(2);
        assertThat(completion.cost()).isEqualByComparingTo(new BigDecimal("0.0000042"));
        assertThat(authHeader.get()).isEqualTo("Bearer test-key");
        assertThat(requestBody.get()).contains("\"stream\":true", "\"model\":\"test/model\"", "\"content\":\"Hi\"");
    }

    @Test
    void reportsErrorsBeforeTheStreamStarts() {
        var client = clientRespondingWith(402, "{\"error\":{\"code\":402,\"message\":\"Insufficient credits\"}}");

        assertThatThrownBy(() -> client.stream("m", List.of(), delta -> {}))
                .isInstanceOfSatisfying(OpenRouterException.class, e -> {
                    assertThat(e.status()).isEqualTo(402);
                    assertThat(e.getMessage()).isEqualTo("Insufficient credits");
                });
    }

    @Test
    void reportsErrorsSentMidStream() {
        var client = clientRespondingWith(200, """
                data: {"choices":[{"delta":{"content":"Par"}}]}

                data: {"error":{"code":"server_error","message":"Provider disconnected"},"choices":[{"finish_reason":"error"}]}

                """);

        assertThatThrownBy(() -> client.stream("m", List.of(), delta -> {}))
                .isInstanceOf(OpenRouterException.class)
                .hasMessage("Provider disconnected");
    }

    private OpenRouterClient clientRespondingWith(int status, String body) {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/api/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", status == 200 ? "text/event-stream" : "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        var baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1");
        return new OpenRouterClient(
                new OpenRouterProperties(baseUrl, "test-key", "Test", "http://localhost"), JsonMapper.builder().build());
    }
}
