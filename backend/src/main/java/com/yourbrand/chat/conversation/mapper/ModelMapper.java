package com.yourbrand.chat.conversation.mapper;

import com.yourbrand.chat.config.ChatProperties;
import com.yourbrand.chat.conversation.dto.response.ModelResponse;
import com.yourbrand.chat.conversation.dto.response.ModelsResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ModelMapper {

    public ModelsResponse toResponse(List<ChatProperties.Model> models, ChatProperties.Model defaultModel) {
        return new ModelsResponse(
                models.stream().map(model -> new ModelResponse(model.id(), model.label())).toList(),
                defaultModel.id());
    }
}
