package net.codesapien;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    public OllamaChatModel visionModel() {
        var ollamaApi = OllamaApi.builder().build();
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaOptions.builder()
                                .model(OllamaModel.LLAVA)
                                .temperature(0.9)
                                .build())
                .build();
    }
}
