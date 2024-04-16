package devoxx.demo.utils;

import com.datastax.astra.client.Collection;
import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.model.Document;
import com.datastax.astra.internal.command.LoggingCommandObserver;
import com.datastax.oss.driver.api.core.CqlSession;
import com.dtsx.astra.sdk.db.AstraDBOpsClient;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;

import java.io.File;
import java.nio.file.Paths;

import static com.datastax.astra.client.model.SimilarityMetric.COSINE;
import static devoxx.demo.devoxx.Utilities.ASTRA_API_ENDPOINT;
import static devoxx.demo.devoxx.Utilities.ASTRA_DB_ID;
import static devoxx.demo.devoxx.Utilities.ASTRA_DB_REGION;
import static devoxx.demo.devoxx.Utilities.ASTRA_KEYSPACE;
import static devoxx.demo.devoxx.Utilities.ASTRA_TOKEN;
import static devoxx.demo.devoxx.Utilities.EMBEDDING_DIMENSION;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_ENDPOINT;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_ID;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_LOCATION;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_PUBLISHER;

public class AbstractDevoxxTestSupport {

    protected LanguageModel getLanguageModel(final String modelName) {
        return VertexAiLanguageModel.builder()
                .project(GCP_PROJECT_ID)
                .endpoint(GCP_PROJECT_ENDPOINT)
                .location(GCP_PROJECT_LOCATION)
                .publisher(GCP_PROJECT_PUBLISHER)
                .modelName(modelName)
                .build();
    }

    protected EmbeddingModel getEmbeddingModel(final String modelName) {
        return VertexAiEmbeddingModel.builder()
                .project(GCP_PROJECT_ID)
                .endpoint(GCP_PROJECT_ENDPOINT)
                .location(GCP_PROJECT_LOCATION)
                .publisher(GCP_PROJECT_PUBLISHER)
                .modelName(modelName)
                .build();
    }

    protected ChatLanguageModel getChatLanguageModel(final String modelName) {
        return VertexAiChatModel.builder()
                .publisher(GCP_PROJECT_PUBLISHER)
                .project(GCP_PROJECT_ID)
                .endpoint(GCP_PROJECT_ENDPOINT)
                .location(GCP_PROJECT_LOCATION)
                .modelName(modelName)
                .temperature(0.7)
                .topK(3)
                .topP(.8)
                .maxRetries(3)
                .maxOutputTokens(2000)
                .build();
    };

    protected ChatLanguageModel getChatLanguageModelChatBison() {
        return getChatLanguageModel("chat-bison");
    }

    protected LanguageModel getLanguageModelTextBison() {
        return getLanguageModel("text-bison");
    }

    protected EmbeddingModel getEmbeddingModelGecko() {
        return getEmbeddingModel("textembedding-gecko@001");
    }


    // ======== Cassandra Session ===========

    CqlSession cqlSession;

    public synchronized CqlSession getCassandraSession() {
        if (cqlSession == null) {
            String secureConnectBundleFolder = System.getProperty("user.home") + File.separator + ".astra" + File.separator + "scb";
            if (!new File(secureConnectBundleFolder).exists()) new File(secureConnectBundleFolder).mkdirs();
            new AstraDBOpsClient(ASTRA_TOKEN)
                    .database(ASTRA_DB_ID)
                    .downloadAllSecureConnectBundles(secureConnectBundleFolder);
            String scb = secureConnectBundleFolder + File.separator + "scb_" + ASTRA_DB_ID + "_" + ASTRA_DB_REGION + ".zip";
            cqlSession = CqlSession.builder()
                    .withAuthCredentials("token", ASTRA_TOKEN)
                    .withCloudSecureConnectBundle(Paths.get(scb))
                    .withKeyspace(ASTRA_KEYSPACE)
                    .build();
        }
        return cqlSession;
    }

    public Collection<Document> getCollectionQuote() {
        Collection<Document> col =  new DataAPIClient(ASTRA_TOKEN)
                .getDatabase(ASTRA_API_ENDPOINT)
                .getCollection("quote_store", Document.class);
        col.registerListener("logger", new LoggingCommandObserver(AbstractDevoxxTestSupport.class));
        return col;
    }


    public Collection<Document> createCollectionQuote() {
        return new DataAPIClient(ASTRA_TOKEN)
                .getDatabase(ASTRA_API_ENDPOINT)
                .createCollection("quote_store", EMBEDDING_DIMENSION, COSINE);
    }

    public Collection<Document> createCollectionRAG() {
        return new DataAPIClient(ASTRA_TOKEN)
                .getDatabase(ASTRA_API_ENDPOINT)
                .createCollection("rag_store", EMBEDDING_DIMENSION, COSINE);
    }

    public Collection<Document> getCollectionRAG() {
        return new DataAPIClient(ASTRA_TOKEN)
                .getDatabase(ASTRA_API_ENDPOINT)
                .getCollection("rag_store");
    }





}
