package com.yourbrand.chat.integration.openrouter;

import com.yourbrand.chat.common.concurrent.CancellationToken;
import com.yourbrand.chat.config.OpenRouterProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenRouter chat completions, streamed over Server-Sent Events or returned whole.
 * See https://openrouter.ai/docs/api-reference/streaming.
 */
@Component
class OpenRouterClient implements ChatCompletionClient {

    private static final String DATA_PREFIX = "data: ";
    private static final Duration COMPLETE_TIMEOUT = Duration.ofSeconds(60);

    private final OpenRouterProperties properties;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    OpenRouterClient(OpenRouterProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
    }

    @Override
    public Completion stream(
            String model, List<ChatMessage> messages, Consumer<String> onDelta, CancellationToken cancellation) {
        var request = request(Map.of("model", model, "messages", messages, "stream", true)).build();
        return interruptibly(cancellation, () -> {
            HttpResponse<Stream<String>> response = send(request, HttpResponse.BodyHandlers.ofLines(), cancellation);

            // Closing the stream closes the connection, which cancels generation upstream.
            try (Stream<String> lines = response.body()) {
                if (response.statusCode() != 200) {
                    throw new OpenRouterException(
                            response.statusCode(), errorMessage(lines.collect(Collectors.joining("\n"))));
                }
                return readEvents(lines.iterator(), onDelta);
            } catch (UncheckedIOException e) {
                cancellation.throwIfCancelled();
                throw new OpenRouterException(502, "The connection to the AI provider was interrupted.");
            }
        });
    }

    @Override
    public CompletionResult complete(
            String model, List<ChatMessage> messages, int maxCompletionTokens, CancellationToken cancellation) {
        var body = Map.of(
                "model", model,
                "messages", messages,
                "stream", false,
                "max_completion_tokens", maxCompletionTokens);
        var request = request(body).timeout(COMPLETE_TIMEOUT).build();
        return interruptibly(cancellation, () -> {
            HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString(), cancellation);
            if (response.statusCode() != 200) {
                throw new OpenRouterException(response.statusCode(), errorMessage(response.body()));
            }

            JsonNode root;
            try {
                root = json.readTree(response.body());
            } catch (RuntimeException e) {
                throw new OpenRouterException(502, "The AI provider returned an invalid response.");
            }
            if (root.has("error")) {
                throw new OpenRouterException(
                        502, root.path("error").path("message").asString("The AI provider returned an error."));
            }
            JsonNode choice = root.path("choices").path(0);
            String content = choice.path("message").path("content").asString("");
            String finishReason = choice.path("finish_reason").asString("");
            return new CompletionResult(completion(content, root.get("usage")), finishReason);
        });
    }

    private HttpRequest.Builder request(Map<String, ?> body) {
        return HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/chat/completions"))
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", properties.appUrl())
                .header("X-Title", properties.appName())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
    }

    private <T> HttpResponse<T> send(
            HttpRequest request, HttpResponse.BodyHandler<T> handler, CancellationToken cancellation) {
        try {
            return http.send(request, handler);
        } catch (HttpTimeoutException e) {
            throw new OpenRouterException(504, "The AI provider took too long to respond. Please try again.");
        } catch (IOException e) {
            cancellation.throwIfCancelled();
            throw new OpenRouterException(502, "Could not reach the AI provider. Please try again.");
        } catch (InterruptedException e) {
            cancellation.throwIfCancelled();
            Thread.currentThread().interrupt();
            throw new OpenRouterException(503, "Request interrupted.");
        }
    }

    /**
     * Runs blocking HTTP work that cancellation interrupts: a pending send is aborted and a blocked body read fails,
     * after which the response body is closed on the way out. The interrupt is cleared before returning.
     */
    private static <T> T interruptibly(CancellationToken cancellation, Supplier<T> work) {
        cancellation.throwIfCancelled();
        Thread worker = Thread.currentThread();
        try (var ignored = cancellation.onCancel(worker::interrupt)) {
            return work.get();
        } finally {
            if (cancellation.isCancelled()) Thread.interrupted();
        }
    }

    private Completion readEvents(Iterator<String> lines, Consumer<String> onDelta) {
        var content = new StringBuilder();
        JsonNode usage = null;

        while (lines.hasNext()) {
            String line = lines.next();
            // Skip blank separators and ": OPENROUTER PROCESSING" keep-alive comments.
            if (!line.startsWith(DATA_PREFIX)) continue;
            String data = line.substring(DATA_PREFIX.length());
            if (data.equals("[DONE]")) break;

            JsonNode chunk = json.readTree(data);
            if (chunk.has("error")) {
                throw new OpenRouterException(502, chunk.path("error").path("message").asString("The AI provider returned an error."));
            }
            String delta = chunk.path("choices").path(0).path("delta").path("content").asString("");
            if (!delta.isEmpty()) {
                content.append(delta);
                onDelta.accept(delta);
            }
            if (chunk.hasNonNull("usage")) usage = chunk.get("usage");
        }

        return completion(content.toString(), usage);
    }

    /** Missing usage fields stay null, so unknown cost is never recorded as zero. */
    private static Completion completion(String content, JsonNode usage) {
        if (usage == null || usage.isNull()) return new Completion(content, null, null, null);
        return new Completion(
                content,
                usage.hasNonNull("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null,
                usage.hasNonNull("completion_tokens") ? usage.get("completion_tokens").asInt() : null,
                usage.hasNonNull("cost") ? usage.get("cost").decimalValue() : null);
    }

    private String errorMessage(String body) {
        try {
            return json.readTree(body).path("error").path("message").asString("The AI provider returned an error.");
        } catch (RuntimeException e) {
            return "The AI provider returned an error.";
        }
    }
}
