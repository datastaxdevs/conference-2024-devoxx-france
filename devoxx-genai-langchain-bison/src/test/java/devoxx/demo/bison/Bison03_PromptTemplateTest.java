package devoxx.demo.bison;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.devoxx.Utilities.GCP_PROJECT_ENDPOINT;
import static com.google.devoxx.Utilities.GCP_PROJECT_ID;
import static com.google.devoxx.Utilities.GCP_PROJECT_LOCATION;
import static com.google.devoxx.Utilities.GCP_PROJECT_PUBLISHER;

public class Bison03_PromptTemplateTest {

    @Test
    public void prompt() {
        LanguageModel model = VertexAiLanguageModel.builder()
                .project(GCP_PROJECT_ID)
                .endpoint(GCP_PROJECT_ENDPOINT)
                .location(GCP_PROJECT_LOCATION)
                .publisher(GCP_PROJECT_PUBLISHER)
                .modelName("text-bison")
                .build();

        PromptTemplate promptTemplate = PromptTemplate.from("""
            Create a recipe for a {{dish}} with the following ingredients: \
            {{ingredients}}, and give it a name.
            """);

        Map<String, Object> variables = new HashMap<>();
        variables.put("dish", "dessert");
        variables.put("ingredients", "strawberries, chocolate, whipped cream");

        Prompt prompt = promptTemplate.apply(variables);
        Response<String> response = model.generate(prompt);
        System.out.println(response.content());
    }
}
