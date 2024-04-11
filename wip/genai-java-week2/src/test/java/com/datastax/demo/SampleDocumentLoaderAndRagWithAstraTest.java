package com.datastax.demo;

import com.dtsx.astra.sdk.utils.TestUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.DocumentType;
import dev.langchain4j.data.document.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.cassandra.AstraDbEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dtsx.astra.sdk.utils.TestUtils.getAstraToken;
import static com.dtsx.astra.sdk.utils.TestUtils.setupDatabase;
import static dev.langchain4j.model.openai.OpenAiModelName.GPT_3_5_TURBO;
import static dev.langchain4j.model.openai.OpenAiModelName.TEXT_EMBEDDING_ADA_002;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SampleDocumentLoaderAndRagWithAstraTest {

    @Value("${astra.database.name}")
    private static String databaseName = "langchain4j";

    @Value("${astra.database.keyspace}")
    private static String keyspace = "langchain4j";

    @Value("${astra.database.table}")
    private static String tableName = "table_story";

    private static String astraToken;
    private static String openAIKey;
    private static String databaseId;
    private static EmbeddingModel embeddingModel;
    private static EmbeddingStore<TextSegment> embeddingStore;

    @BeforeAll
    public static void checkPreRequisites() throws InterruptedException {
        // Initialization
        astraToken = getAstraToken();
        openAIKey = System.getenv("OPENAI_API_KEY");
        databaseId = setupDatabase(databaseName, keyspace);

        // Given
        assertNotNull(openAIKey);
        assertNotNull(databaseId);
        assertNotNull(astraToken);
        log.info("Pre-requisites are met.");
    }

    @Test
    @DisplayName("01. Should initialize the model and store")
    @EnabledIfEnvironmentVariable(named = "ASTRA_DB_APPLICATION_TOKEN", matches = "Astra.*")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk.*")
    public void shouldInitializationModelAndStore() {

        // Embedding model (OpenAI)
        embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openAIKey)
                .modelName(TEXT_EMBEDDING_ADA_002)
                .timeout(ofSeconds(15))
                .logRequests(true)
                .logResponses(true)
                .build();
        log.info("[OK] - Embeddings Model (OpenAI)");

        // Embed the document and it in the store
        embeddingStore = AstraDbEmbeddingStore.builder()
                .token(astraToken)
                .database(databaseId, TestUtils.TEST_REGION)
                .table(keyspace, tableName)
                .vectorDimension(1536)
                .build();
        log.info("[OK] - Embeddings Store (Astra)");
    }

    @Test
    @DisplayName("02. Should Ingest a document")
    @EnabledIfEnvironmentVariable(named = "ASTRA_DB_APPLICATION_TOKEN", matches = "Astra.*")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk.*")
    void should_Ingest_Document() {

        Path path = new File(getClass().getResource("/story-about-happy-carrot.txt").getFile()).toPath();
        Document document = FileSystemDocumentLoader.loadDocument(path, DocumentType.TXT);
        DocumentSplitter splitter = DocumentSplitters
                .recursive(100, 10,
                        new OpenAiTokenizer(GPT_3_5_TURBO));
        log.info("[OK] - Document SPLITTER");

        // Ingest method 2
        EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build().ingest(document);
        log.info("[OK] - DOCUMENT PROCESSED");
    }

    @Test
    @DisplayName("03. Should Chat Completion")
    @EnabledIfEnvironmentVariable(named = "ASTRA_DB_APPLICATION_TOKEN", matches = "Astra.*")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk.*")
    void should_chat_completion() {

        // --------- RAG -------------

        // Specify the question you want to ask the model
        String question = "Who is Charlie?";

        // Embed the question
        Response<Embedding> questionEmbedding = embeddingModel.embed(question);

        // Find relevant embeddings in embedding store by semantic similarity
        // You can play with parameters below to find a sweet spot for your specific use case
        int maxResults = 3;
        double minScore = 0.8;
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings =
                embeddingStore.findRelevant(questionEmbedding.content(), maxResults, minScore);

        // --------- Chat Template -------------

        // Create a prompt for the model that includes question and relevant embeddings
        PromptTemplate promptTemplate = PromptTemplate.from(
                "Answer the following question to the best of your ability:\n"
                        + "\n"
                        + "Question:\n"
                        + "{{question}}\n"
                        + "\n"
                        + "Base your answer on the following information:\n"
                        + "{{information}}");

        String information = relevantEmbeddings.stream()
                .map(match -> match.embedded().text())
                .collect(joining("\n\n"));

        Map<String, Object> variables = new HashMap<>();
        variables.put("question", question);
        variables.put("information", information);

        Prompt prompt = promptTemplate.apply(variables);

        // Send the prompt to the OpenAI chat model
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(openAIKey)
                .modelName(GPT_3_5_TURBO)
                .temperature(0.7)
                .timeout(ofSeconds(15))
                .maxRetries(3)
                .logResponses(true)
                .logRequests(true)
                .build();

        Response<AiMessage> aiMessage = chatModel.generate(prompt.toUserMessage());

        // See an answer from the model
        String answer = aiMessage.content().text();
        System.out.println(answer);
    }
}