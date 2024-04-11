package devoxx.demo;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.astradb.AstraDbEmbeddingStore;
import io.stargate.sdk.data.domain.query.Filter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static io.stargate.sdk.http.domain.FilterOperator.EQUALS_TO;

@Slf4j
public class AstraDbEmbeddingStoreRagTest extends AbstractAstraDbTests {

    @Test
    public void testAstraDbEmbeddingStore() throws FileNotFoundException {

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

        // ---------------------
        // ------- RAG ---------
        // ---------------------

        // Specify the question you want to ask the model
        String question = "Who is Johnny ?";

        // Build Naive RAG Context with metadata filtering on document_id
        String ragContext = buildRagContext(question,"doc-johnny", embeddingModel, embeddingStore);

        // Prompting with Rag context
        PromptTemplate promptTemplate = PromptTemplate.from(
                "Answer the following question to the best of your ability:\n"
                        + "Question:\n"
                        + "{{question}}\n"
                        + "Base your answer on the following information:\n"
                        + "{{information}}");
        Map<String, Object> variables = new HashMap<>();
        variables.put("question", question);
        variables.put("information", ragContext);

        Prompt prompt = promptTemplate.apply(variables);
        log.info("Final Prompt {}", prompt.text());

        Response<AiMessage> aiMessage = chatModel.generate(prompt.toUserMessage());

        // See an answer from the model
        String answer = aiMessage.content().text();
        log.info("Answer from the model: {}", answer);

        // Clean up
        // deleteDocumentById(collection, documentId1);
        // deleteDocumentById(collection, documentId2);
    }

    private String buildRagContext(String question, String docId, EmbeddingModel embeddingModel, AstraDbEmbeddingStore embeddingStore) {

        // Embed the question
        Response<Embedding> questionEmbedding = embeddingModel.embed(question);

        /*
         * ------------------------------------
         * Search with metadata filtering
         * @see https://awesome-astra.github.io/docs/pages/develop/sdk/astra-db-client-java/#find-one
         * ------------------------------------
         */
        Filter filterByDocumentId = new Filter().where("document_id", EQUALS_TO, docId);

        // Limit result to keep relevant informations
        int maxResults = 5;

        // Minimum score to keep the result
        double minScore = 0.3;

        log.info("RAG Search with the Store");
        return String.join(",", embeddingStore
                .findRelevant(questionEmbedding.content(), filterByDocumentId,  maxResults, minScore)
                .stream()
                .map(emb -> emb.embedded().text())
                .toList());
    }

}
