package com.example.querysence.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public float[] generateEmbedding(String text) {
        return embeddingModel.embed(text);
    }

    public Map<String, float[]> generateBatch(List<String> texts) {
        return texts.stream().collect(Collectors.toMap(text -> text, this::generateEmbedding));
    }
}
