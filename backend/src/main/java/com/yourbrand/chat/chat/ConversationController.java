package com.yourbrand.chat.chat;

import com.yourbrand.chat.chat.ChatDtos.ConversationResponse;
import com.yourbrand.chat.chat.ChatDtos.CreateConversationRequest;
import com.yourbrand.chat.chat.ChatDtos.MessageResponse;
import com.yourbrand.chat.chat.ChatDtos.RenameRequest;
import com.yourbrand.chat.chat.ChatDtos.SendMessageRequest;
import com.yourbrand.chat.user.AppUserPrincipal;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/conversations")
class ConversationController {

    private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(5);

    private final ChatService chat;
    private final ExecutorService chatStreamExecutor;

    ConversationController(ChatService chat, ExecutorService chatStreamExecutor) {
        this.chat = chat;
        this.chatStreamExecutor = chatStreamExecutor;
    }

    @GetMapping
    List<ConversationResponse> list(@AuthenticationPrincipal AppUserPrincipal user) {
        return chat.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ConversationResponse create(
            @AuthenticationPrincipal AppUserPrincipal user, @RequestBody CreateConversationRequest request) {
        return chat.create(user.id(), request.model());
    }

    @PatchMapping("/{id}")
    ConversationResponse rename(
            @AuthenticationPrincipal AppUserPrincipal user,
            @PathVariable UUID id,
            @Valid @RequestBody RenameRequest request) {
        return chat.rename(user.id(), id, request.title());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        chat.delete(user.id(), id);
    }

    @GetMapping("/{id}/messages")
    List<MessageResponse> messages(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        return chat.messages(user.id(), id);
    }

    /** Validation and budget errors return plain JSON; once streaming starts, errors arrive as SSE events. */
    @PostMapping(path = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> send(
            @AuthenticationPrincipal AppUserPrincipal user,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        var reply = chat.prepare(user.id(), id, request.content(), request.model());
        var emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
        chatStreamExecutor.execute(() -> chat.streamReply(reply, emitter));
        return ResponseEntity.ok()
                // Ask proxies not to buffer or compress the stream (gzip would hold chunks back).
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache, no-transform")
                .body(emitter);
    }
}
