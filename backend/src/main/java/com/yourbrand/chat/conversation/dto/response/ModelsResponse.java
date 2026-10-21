package com.yourbrand.chat.conversation.dto.response;

import java.util.List;

public record ModelsResponse(List<ModelResponse> models, String defaultModel) {}
