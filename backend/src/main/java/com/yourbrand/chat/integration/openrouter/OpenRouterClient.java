package com.yourbrand.chat.integration.openrouter;

import com.yourbrand.chat.config.OpenRouterProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenRouter chat completions over Server-Sent Events.
 * See https://openrouter.ai/docs/api-reference/streaming.
 */
@Component
class OpenRouterClient implements ChatCompletionClient {

    private static final String DATA_PREFIX = "data: ";

    private final OpenRouterProperties properties;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    OpenRouterClient(OpenRouterProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
    }

    @Override
    public Completion stream(String model, List<ChatMessage> messages, Consumer<String> onDelta) {
        var body = Map.of("model", model, "messages", messages, "stream", true);
        var request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/chat/completions"))
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", properties.appUrl())
                .header("X-Title", properties.appName())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();

        HttpResponse<Stream<String>> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofLines());
        } catch (IOException e) {
            throw new OpenRouterException(502, "Could not reach the AI provider. Please try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenRouterException(503, "Request interrupted.");
        }

        // Closing the stream closes the connection, which cancels generation upstream.
        try (Stream<String> lines = response.body()) {
            if (response.statusCode() != 200) {
                throw new OpenRouterException(
                        response.statusCode(), errorMessage(lines.collect(Collectors.joining("\n"))));
            }
            return readEvents(lines.iterator(), onDelta);
        } catch (UncheckedIOException e) {
            throw new OpenRouterException(502, "The connection to the AI provider was interrupted.");
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

        if (usage == null) return new Completion(content.toString(), null, null, null);
        return new Completion(
                content.toString(),
                usage.path("prompt_tokens").asInt(),
                usage.path("completion_tokens").asInt(),
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
