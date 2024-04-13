package devoxx.demo.gemini;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ChatSession;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.junit.jupiter.api.Test;

import static com.google.devoxx.Utilities.GCP_PROJECT_ID;
import static com.google.devoxx.Utilities.GCP_PROJECT_LOCATION;

public class Demo01_VertexClientChat {

    @Test
    public void testChat() throws Exception {
        try (VertexAI vertexAI = new VertexAI(GCP_PROJECT_ID, GCP_PROJECT_LOCATION)) {
            GenerateContentResponse response;

            GenerativeModel model = new GenerativeModel("gemini-pro", vertexAI);
            ChatSession chatSession = new ChatSession(model);

            response = chatSession.sendMessage("Hello.");
            System.out.println(ResponseHandler.getText(response));

            response = chatSession.sendMessage("What are all the colors in a rainbow?");
            System.out.println(ResponseHandler.getText(response));

            response = chatSession.sendMessage("Why does it appear when it rains?");
            System.out.println(ResponseHandler.getText(response));
        }
    }
}
