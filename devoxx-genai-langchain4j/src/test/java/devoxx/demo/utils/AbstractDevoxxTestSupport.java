package devoxx.demo.utils;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;

import java.util.UUID;

import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_ENDPOINT;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_ID;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_LOCATION;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_PUBLISHER;

public class AbstractDevoxxTestSupport {

    protected static String astraToken = System.getenv("ASTRA_DB_APPLICATION_TOKEN");

    protected static UUID astraDatabaseId = UUID.fromString("bace77c5-80ea-4bc4-a0f4-529121918cd4");

    protected static String astraDatabaseKeyspace = "default_keyspace";

    protected static String astraDatabaseRegion = "us-east1";

    protected static String astraEndpoint = "https://"+ astraDatabaseId.toString()+"-"+astraDatabaseRegion+".apps.astra.datastax.com/api/json";

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


}
