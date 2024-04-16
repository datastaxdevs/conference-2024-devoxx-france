package devoxx.demo.utils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dtsx.astra.sdk.db.AstraDBOpsClient;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;

import java.io.File;
import java.nio.file.Paths;

import static devoxx.demo.devoxx.Utilities.ASTRA_DB_ID;
import static devoxx.demo.devoxx.Utilities.ASTRA_DB_REGION;
import static devoxx.demo.devoxx.Utilities.ASTRA_KEYSPACE;
import static devoxx.demo.devoxx.Utilities.ASTRA_TOKEN;
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



}
