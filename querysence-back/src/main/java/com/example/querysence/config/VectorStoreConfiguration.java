package com.example.querysence.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pinecone.PineconeVectorStore;
import org.springframework.beans.factory.annotation.Value;

public class VectorStoreConfiguration {

    @Value("${spring.ai.vectorstore.pinecone.api-key}")
    private String pineconeApiKey;

    @Value("${spring.ai.vectorstore.pinecone.index-name}")
    private String indexName;

    @Value("${spring.ai.vectorstore.pinecone.namespace}")
    private String namespace;

    public PineconeVectorStore pineconeVectorStore(EmbeddingModel embeddingModel) {
        return PineconeVectorStore.builder(embeddingModel)
                .apiKey(pineconeApiKey)
                .indexName(indexName)
                .namespace(namespace)
                .build();
    }
}
