package devoxx.demo;

import com.dtsx.astra.sdk.AstraDB;
import com.dtsx.astra.sdk.AstraDBCollection;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.astradb.AstraDbEmbeddingStore;
import io.stargate.sdk.data.domain.query.DeleteQuery;
import io.stargate.sdk.data.domain.query.DeleteResult;
import io.stargate.sdk.data.domain.query.Filter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_3_5_TURBO;
import static io.stargate.sdk.data.domain.SimilarityMetric.cosine;
import static io.stargate.sdk.http.domain.FilterOperator.EQUALS_TO;


@Slf4j
abstract class AbstractAstraDbTests {

    String astraToken     = "AstraCS:iLPiNPxSSIdefoRdkTWCfWXt:2b360d096e0e6cb732371925ffcc6485541ff78067759a2a1130390e231c2c7a";
    String apiEndpoint    = "https://bace77c5-80ea-4bc4-a0f4-529121918cd4us-east1.apps.astra.datastax.com/api/json";
    String keyspace       = "default_keyspace";
    String collectionName = "demo_collection";
    int vectorDimension = 1536;

    protected EmbeddingModel initializeEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL)
                .build();
    }

    protected ChatLanguageModel initializeChatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(GPT_3_5_TURBO)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(15))
                .maxRetries(3)
                .logResponses(true)
                .logRequests(true)
                .build();
    }

    protected AstraDbEmbeddingStore initializeEmbeddingStore() {
        /*
         * Accessing Astra Database
         *
         * Operations at DB level:
         * @see https://awesome-astra.github.io/docs/pages/develop/sdk/astra-db-client-java/#working-with-collections
         */
        AstraDB sampleDb = new AstraDB(astraToken, apiEndpoint, keyspace);
        log.info("You are connected to Astra on keyspace '{}'", sampleDb.getCurrentKeyspace());

        /*
         * Accessing a collection (create if not exists)
         *
         * Operations at Collection level:
         * @see https://awesome-astra.github.io/docs/pages/develop/sdk/astra-db-client-java/#working-with-documents
         */
        AstraDBCollection collection = sampleDb.createCollection(collectionName, vectorDimension, cosine);
        log.info("Your collection '{}' has been created (if needed) ", collectionName);

        // Flushing Collection
        collection.deleteAll();
        log.info("Your collection '{}' has been flushed", collectionName);

        // Langchain4j Embedding Store
        return new AstraDbEmbeddingStore(collection);
    }

    protected void ingestDocument(String docId, String documentName, EmbeddingModel embeddingModel, AstraDbEmbeddingStore embeddingStore) {

        // Splitter is important, not the whole document fit a single embedding
        DocumentSplitter splitter = DocumentSplitters
                .recursive(100, 10,
                        new OpenAiTokenizer(GPT_3_5_TURBO.toString()));

        // Different loader per document extensions
        DocumentParser documentParser = new TextDocumentParser();

        // Load document binary content as inputstream
        File myFile = new File(getClass().getResource("/" + documentName).getFile());
        Document myDocument = FileSystemDocumentLoader.loadDocument(myFile.toPath(), documentParser);

        // Langchain4j Ingestor
        EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .textSegmentTransformer(ts -> {
                    // '_id' is the technical identifier
                    ts.metadata().add("document_id", docId);
                    ts.metadata().add("document_format", "text");
                    return ts;
                }).build().ingest(myDocument);

        log.info("Document '{}' has been ingested with id {}", documentName, docId);
    }

    protected DeleteResult deleteDocumentById(AstraDBCollection collection, UUID documentId) {
        Filter deleteByid = new Filter().where("document_id", EQUALS_TO, documentId.toString());
        return collection.deleteMany(DeleteQuery.builder().filter(deleteByid).build());
    }

}
