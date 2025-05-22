package com.alibaba.cloud.ai.example.manus.util;

import cn.hutool.core.util.StrUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultChatGenerationMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Utils {
    private static AssistantMessage mergeGenerations(List<Generation> generations) {
        var combinedContent = new StringBuilder();
        var combinedToolCalls = new ArrayList<AssistantMessage.ToolCall>();
        var combinedMetadata = new HashMap<String, Object>();

        for (Generation gen: generations) {
            AssistantMessage output = gen.getOutput();
            if (StrUtil.isNotBlank(output.getText())) {
                combinedContent.append(output.getText());
            }
            combinedMetadata.putAll(output.getMetadata());
            combinedToolCalls.addAll(output.getToolCalls());
        }

        return new AssistantMessage(combinedContent.toString(), combinedMetadata, combinedToolCalls);
    }

    private static ChatResponseMetadata mergeMetadatas(List<ChatResponseMetadata> metadatas) {
        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;

        for (ChatResponseMetadata metadata: metadatas) {
            totalPromptTokens += metadata.getUsage().getPromptTokens();
            totalCompletionTokens += metadata.getUsage().getCompletionTokens();
        }

        return ChatResponseMetadata.builder().usage(new DefaultUsage(totalPromptTokens, totalCompletionTokens, totalPromptTokens + totalCompletionTokens)).build();
    }

    private static Mono<ChatResponse> getMonoChatResponse(Flux<ChatResponse> responseFlux) {
        var allGenerations = new ArrayList<Generation>();
        var metadatas = new ArrayList<ChatResponseMetadata>();

        return responseFlux
                .doOnNext(chatResponse -> {
                    allGenerations.addAll(chatResponse.getResults());
                    metadatas.add(chatResponse.getMetadata());
                })
                .then(Mono.fromCallable(() -> {
                            AssistantMessage mergedMessage = mergeGenerations(allGenerations);
                            ChatResponseMetadata mergedMetaData = mergeMetadatas(metadatas);

                            return new ChatResponse(List.of(new Generation(mergedMessage)), mergedMetaData);
                        })
                );
    }

    public static ChatResponse getFlowChatResponse(Flux<ChatResponse> responseFlux) throws Exception {
        Mono<ChatResponse> chatResponseMono = getMonoChatResponse(responseFlux);
        var list = new ArrayList<ChatResponse>();
        var latch = new CountDownLatch(1);
        chatResponseMono.subscribe(mergedResponse -> {
            list.add(mergedResponse);
            latch.countDown();
        });
        latch.await();
        return list.get(0);
    }
}
