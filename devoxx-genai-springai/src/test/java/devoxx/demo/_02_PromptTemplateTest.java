package devoxx.demo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * This test demonstrates how to use a prompt template to generate a prompt for a language model.
 * <a href="https://mustache.github.io/">MUSTACHE</a>
 */
@SpringBootTest
public class _02_PromptTemplateTest {

    @Autowired
    private VertexAiGeminiChatClient client;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;

    @Test
    public void prompt() {

        PromptTemplate promptTemplate = new PromptTemplate("""
            Explain me why a {{profile}} should attend conference {{conference}}.
            The conference is on {{current_date}} at {{current_time}} with {{current_date_time}}
            """);

        Map<String, Object> variables = new HashMap<>();
        variables.put("profile", "Java Developer");
        variables.put("conference", "Devoxx France");

        Prompt prompt = promptTemplate.create(variables);
        ChatResponse response = client.call(prompt);
        System.out.println(response.getResult().getOutput().getContent());


    }
}
