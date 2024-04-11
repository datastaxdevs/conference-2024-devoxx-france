package com.datastax.demo;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dtsx.astra.sdk.cassio.MetadataVectorCassandraTable;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenerativeAiConfiguration {

    @Value("${generative-ai.table-name}")
    public String vectorTableName;

    // ----- OpenAI LLM Provider Keys

    @Value("${generative-ai.llm-model.dimension}")
    public int llmModelDimension;

    @Value("${generative-ai.llm-model.embeddings}")
    public String llmModelNameEmbeddings;

    @Value("${generative-ai.llm-model.chat-completion}")
    public String llmModelNameChatCompletion;

    @Bean
    public OpenAiService openAiClient() {
        return new OpenAiService(System.getenv("OPENAI_API_KEY"));
    }

    @Bean
    public MetadataVectorCassandraTable vectorTable(CqlSession cqlSession) {
        return new MetadataVectorCassandraTable(cqlSession, cqlSession.getKeyspace().get().toString(),
                vectorTableName, llmModelDimension);
    }

}
