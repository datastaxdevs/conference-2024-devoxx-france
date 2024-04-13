package devoxx.demo.bison;

import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;
import org.junit.jupiter.api.Test;

import static com.google.devoxx.Utilities.GCP_PROJECT_ENDPOINT;
import static com.google.devoxx.Utilities.GCP_PROJECT_ID;
import static com.google.devoxx.Utilities.GCP_PROJECT_LOCATION;
import static com.google.devoxx.Utilities.GCP_PROJECT_PUBLISHER;

public class Bison02_ExtractTextTest {

    @Test
    public void test() {

        LanguageModel model = VertexAiLanguageModel.builder()
                .project(GCP_PROJECT_ID)
                .endpoint(GCP_PROJECT_ENDPOINT)
                .location(GCP_PROJECT_LOCATION)
                .publisher(GCP_PROJECT_PUBLISHER)
                .modelName("text-bison")
                .build();

        Response<String> response = model.generate("""
            Extract the name and age of the person described below.
            Return a JSON document with a "name" and an "age" property, \
            following this structure: {"name": "John Doe", "age": 34}
            Return only JSON, without any markdown markup surrounding it.
            Here is the document describing the person:
            ---
            Anna is a 23 year old artist based in Brooklyn, New York. She was born and 
            raised in the suburbs of Chicago, where she developed a love for art at a 
            young age. She attended the School of the Art Institute of Chicago, where 
            she studied painting and drawing. After graduating, she moved to New York 
            City to pursue her art career. Anna's work is inspired by her personal 
            experiences and observations of the world around her. She often uses bright 
            colors and bold lines to create vibrant and energetic paintings. Her work 
            has been exhibited in galleries and museums in New York City and Chicago.    
            ---
            JSON: 
            """
        );
        System.out.println(response.content());

    }
}
