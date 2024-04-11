package devoxx.demo;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiImageModel;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.devoxx.Utilities.GCP_PROJECT_ENDPOINT;
import static com.google.devoxx.Utilities.GCP_PROJECT_ID;
import static com.google.devoxx.Utilities.GCP_PROJECT_LOCATION;
import static com.google.devoxx.Utilities.GCP_PROJECT_PUBLISHER;
import static org.assertj.core.api.Assertions.assertThat;

class SayHelloTextBisonTest {

    @Test
    public void shouldSayHelloToLLM() {

        LanguageModel llm = VertexAiLanguageModel.builder()
                .project(GCP_PROJECT_ID)
                .endpoint(GCP_PROJECT_ENDPOINT)
                .location(GCP_PROJECT_LOCATION)
                .publisher(GCP_PROJECT_PUBLISHER)
                .modelName("text-bison")
                .build();

        Response<String> response = llm.generate("Hello, LLM!");
        System.out.println(response.content());
    }

    @Test
    public void shouldFineTuneYourRequest() {

        LanguageModel llm =  VertexAiLanguageModel.builder()
                .project(GCP_PROJECT_ID)
                .endpoint(GCP_PROJECT_ENDPOINT)
                .location(GCP_PROJECT_LOCATION)
                .modelName("text-bison")
                .temperature(0.7)
                .topK(3)
                .topP(.8) // no both at same time
                .maxRetries(3)
                .maxOutputTokens(2000)
                .build();
        Response<String> response = llm.generate("What it the capital of France?");
        System.out.println(response.content());
    }
}
