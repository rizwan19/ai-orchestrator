package com.yourbrand.chat.conversation.controller;

import com.yourbrand.chat.conversation.dto.response.ModelsResponse;
import com.yourbrand.chat.conversation.mapper.ModelMapper;
import com.yourbrand.chat.conversation.service.ModelCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
class ModelController {

    private final ModelCatalogService models;
    private final ModelMapper mapper;

    ModelController(ModelCatalogService models, ModelMapper mapper) {
        this.models = models;
        this.mapper = mapper;
    }

    @GetMapping
    ModelsResponse list() {
        return mapper.toResponse(models.models(), models.defaultModel());
    }
}
