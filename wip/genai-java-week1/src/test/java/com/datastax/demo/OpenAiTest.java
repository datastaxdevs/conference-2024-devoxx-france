package com.datastax.demo;

import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class OpenAiTest {

    /**
     * Logger.
     */
    static Logger log = LoggerFactory.getLogger(GenerativeAITest.class);

    /**
     * Keys in application.yml
     */
    @Value("${generative-ai.llm-model.embeddings}")
    public String llmModelNameEmbeddings;

    @Autowired
    public OpenAiService openAIClient;

    /**
     * A test call for embeddings.
     * Quickly check how one can get the embedding vectors for a list of input texts.
     */
    @Test
    public void shouldTestOpenAICreateEmbeddings() {
        List<Embedding> result = openAIClient
                .createEmbeddings(EmbeddingRequest.builder()
                        .model(llmModelNameEmbeddings)
                        .input(Arrays.asList("This is a sentence","A second sentence"))
                        .build()).getData();
        log.info("len(result.data)={}", result.size());
        log.info("result.data[0].embedding={}", result.get(0).getEmbedding().subList(0,50));
        log.info("len(result.data[0].embedding)={}", result.get(0).getEmbedding().size());
    }

}
