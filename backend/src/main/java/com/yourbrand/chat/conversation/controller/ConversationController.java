package com.yourbrand.chat.conversation.controller;

import com.yourbrand.chat.conversation.dto.request.ConversationRequest;
import com.yourbrand.chat.conversation.dto.request.RenameConversationRequest;
import com.yourbrand.chat.conversation.dto.request.SendMessageRequest;
import com.yourbrand.chat.conversation.dto.response.ConversationResponse;
import com.yourbrand.chat.conversation.dto.response.MessagePageResponse;
import com.yourbrand.chat.conversation.mapper.ConversationMapper;
import com.yourbrand.chat.conversation.mapper.MessageMapper;
import com.yourbrand.chat.conversation.service.ChatReplyService;
import com.yourbrand.chat.conversation.service.ConversationService;
import com.yourbrand.chat.conversation.service.MessageService;
import com.yourbrand.chat.conversation.web.SseReplyListener;
import com.yourbrand.chat.usage.mapper.UsageMapper;
import com.yourbrand.chat.user.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
class ConversationController {

    private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(5);

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatReplyService chatReplyService;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UsageMapper usageMapper;
    private final ExecutorService executorService;

    @GetMapping
    List<ConversationResponse> list(@AuthenticationPrincipal AppUserPrincipal user) {
        return conversationService.getList(user.id()).stream().map(conversationMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ConversationResponse create(
            @AuthenticationPrincipal AppUserPrincipal user, @RequestBody ConversationRequest request) {
        return conversationMapper.toResponse(conversationService.create(user.id(), request.model()));
    }

    @PatchMapping("/{id}")
    ConversationResponse rename(
            @AuthenticationPrincipal AppUserPrincipal user,
            @PathVariable UUID id,
            @Valid @RequestBody RenameConversationRequest request) {
        return conversationMapper.toResponse(conversationService.rename(user.id(), id, request.title()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        conversationService.delete(user.id(), id);
    }

    @GetMapping("/{id}/messages")
    MessagePageResponse messages(
            @AuthenticationPrincipal AppUserPrincipal user,
            @PathVariable UUID id,
            @RequestParam(name = "cursor", required = false) UUID cursor,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return messageMapper.toPageResponse(messageService.getPage(user.id(), id, cursor, size));
    }

    @PostMapping(path = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> send(
            @AuthenticationPrincipal AppUserPrincipal user,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        var reply = chatReplyService.acceptUserMessage(user.id(), id, request.content(), request.model());
        var emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
        var listener = new SseReplyListener(emitter, messageMapper, usageMapper);
        executorService.execute(() -> chatReplyService.generate(reply, listener));
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache, no-transform")
                .body(emitter);
    }
}
