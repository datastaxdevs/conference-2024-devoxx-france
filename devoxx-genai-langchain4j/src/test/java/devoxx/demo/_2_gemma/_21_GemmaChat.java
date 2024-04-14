package devoxx.demo._2_gemma;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

/**
 * OLLAMA
 *
 * Installation
 * https://ollama.com/download/mac
 *
 * Install & Test Gemma
 * ollama run gemma:7b
 * ollama list
 *
 * Test Gemma
 *
 *
 curl http://localhost:11434/api/generate -d '{
    "model": "gemma:7b",
    "prompt": "Why is the sky blue?"
  }'

 clear
 curl -N -s http://localhost:11434/api/generate -d '{
 "model": "gemma:7b",
 "prompt": "Why is the sky b
 clear
 curl -N -s http://localhost:11434/api/generate -d '{
 "model": "gemma:7b",
 "prompt": "Why is the sky blue?"
 }' | while IFS= read -r line; do
 echo "$line" |  jq -r '.response' 2>/dev/null | tr '\n' ' ' | cut -b 1-50
 donelue?"
 }' | while IFS= read -r line; do
 echo "$line" |  jq -r '.response' 2>/dev/null | tr '\n' ' ' | cut -b 1-50
 done

 */
public class _21_GemmaChat {

    @Test
    public void talkWithGemma() {
        ChatLanguageModel gemma = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434/api/")
                .modelName("gemma:7b")
                .build();

        System.out.println(gemma.generate("Present yourself , the name of the model, who trained you ?"));
    }
}
