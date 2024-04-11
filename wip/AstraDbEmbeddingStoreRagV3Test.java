package devoxx.demo;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.astradb.AstraDbEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class AstraDbEmbeddingStoreRagV3Test extends AbstractAstraDbTests {

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
        // ------- RAG     -------------------
        // -----------------------------------

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // on each interaction we will retrieve the 2 most relevant segments
                .minScore(0.5) // we want to retrieve segments at least somewhat similar to user query
                .build();

        PromptTemplate promptTemplate = PromptTemplate.from(
                "Answer the following question to the best of your ability:\n"
                        + "Question:\n"
                        + "{{userMessage}}\n"
                        + "Base your answer on the following information:\n"
                        + "{{contents}}");

        // Each retrieved segment should include "file_name" and "index" metadata values in the prompt
        ContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(promptTemplate)
                .metadataKeysToInclude(Arrays.asList("file_name", "index", "color"))
                .build();

        List<Content> contents = new ArrayList<>();
        TextSegment segment = new TextSegment("color", new Metadata(Map.of("color", "blue")));
        Content content = new Content(segment);
        contentInjector.inject(contents, new UserMessage("Who is Johnny ?"));

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .contentAggregator()
                .build();

        SampleAgent agent2 = AiServices.builder(SampleAgent.class)
                .chatLanguageModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();

        log.info("Answer from the model:\n {}", agent2.answer("Who is Johnny ?"));
    }

    interface SampleAgent {
        String answer(String query);
    }
}
