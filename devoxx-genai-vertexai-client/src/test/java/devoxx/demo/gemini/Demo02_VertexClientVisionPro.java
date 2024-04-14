package devoxx.demo.gemini;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_ID;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_LOCATION;
import static org.assertj.core.api.Assertions.assertThat;

public class Demo02_VertexClientVisionPro {

    @Test
    public void testVision() throws Exception {

        // Load the image
        byte[] imageBytes;// = Files.readAllBytes(Paths.get(imageName));

        String resourcePath = "/img1.png"; // Resource path in the classpath

        try (InputStream is = Demo02_VertexClientVisionPro.class.getResourceAsStream(resourcePath)) {
            assertThat(is).isNotNull();
            imageBytes = is.readAllBytes();
            System.out.println("Image bytes read successfully. Length: " + imageBytes.length);
            try (VertexAI vertexAI = new VertexAI(GCP_PROJECT_ID, GCP_PROJECT_LOCATION)) {
                GenerativeModel model = new GenerativeModel("gemini-pro-vision", vertexAI);
                GenerateContentResponse response = model.generateContent(
                        ContentMaker.fromMultiModalData(
                                "What is this image about?",
                                PartMaker.fromMimeTypeAndData("image/jpg", imageBytes)
                        ));

                System.out.println(ResponseHandler.getText(response));
            }
        } catch (IOException e) {
            System.out.println("Error reading the image file.");
        }
    }
}
