package com.alibaba.cloud.ai.example.manus.llm;

import cn.hutool.core.util.StrUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class LlmTestService {
    @Autowired
    ChatModel chatModel;

    public String chat(String msg) {
        Flux<ChatResponse> response = ChatClient.create(chatModel).prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(msg)
                .stream()
                .chatResponse();
        List<ChatResponse> list = response.collectList().block();
        return "true";
    }

}
