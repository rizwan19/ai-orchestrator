package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.exception.BadRequestException;
import com.yourbrand.chat.config.ChatProperties;
import java.util.List;
import org.springframework.stereotype.Service;

/** The curated models users may chat with ({@code app.chat.models}; the first is the default). */
@Service
public class ModelCatalogService {

    private final ChatProperties properties;

    ModelCatalogService(ChatProperties properties) {
        this.properties = properties;
    }

    public List<ChatProperties.Model> models() {
        return properties.models();
    }

    public ChatProperties.Model defaultModel() {
        return properties.defaultModel();
    }

    public String requireSupported(String model) {
        if (!properties.supports(model)) {
            throw new BadRequestException("This model is not available.");
        }
        return model;
    }
}
