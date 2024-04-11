package devoxx.demo;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.astradb.AstraDbEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class AstraDbEmbeddingStoreRagV2Test extends AbstractAstraDbTests {

    @Test
    public void testAstraDbEmbeddingStore() {

        // -------------------------------
        // --- INIT MODELS AND STORE -----
        // -------------------------------

        // Create Embedding Store with AstraDB
        AstraDbEmbeddingStore embeddingStore = initializeEmbeddingStore();

        // OpenAI model
        EmbeddingModel embeddingModel = initializeEmbeddingModel();

        // Send the prompt to the OpenAI chat model
        ChatLanguageModel chatModel = initializeChatLanguageModel();

        // ------------------------
        // ------- INGEST ---------
        // ------------------------

        ingestDocument("doc-johnny", "johnny.txt", embeddingModel, embeddingStore);
        ingestDocument("doc-carrot", "story-about-happy-carrot.txt", embeddingModel, embeddingStore);

        // -----------------------------------
        // ------- RAG (NO METADATA) ---------
        // -----------------------------------

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // on each interaction we will retrieve the 2 most relevant segments
                .minScore(0.5) // we want to retrieve segments at least somewhat similar to user query
                .build();

        SampleAgent agent = AiServices.builder(SampleAgent.class)
                .chatLanguageModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // Specify the question you want to ask the model
        String answer = agent.answer("Who is Johnny ?");
        log.info("Answer from the model:\n {}", answer);
    }

    interface SampleAgent {
        String answer(String query);
    }
}
