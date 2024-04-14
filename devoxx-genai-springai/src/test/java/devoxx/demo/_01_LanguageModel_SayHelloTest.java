package devoxx.demo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class _01_LanguageModel_SayHelloTest {

    @Autowired
    private VertexAiGeminiChatClient client;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;

    @Test
    void roleTest() {
        String request = "Tell me about 3 famous pirates from the Golden Age of Piracy and why they did.";
        String name = "Bob";
        String voice = "pirate";
        UserMessage userMessage = new UserMessage(request);
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", name, "voice", voice));
        Prompt prompt = new Prompt(List.of(userMessage, systemMessage));
        ChatResponse response = client.call(prompt);
        System.out.println(response.getResult().getOutput().getContent());
    }

}
