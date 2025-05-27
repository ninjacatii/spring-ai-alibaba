package com.alibaba.cloud.ai.example.manus.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultChatGenerationMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class Utils {
    public static ChatResponse getChatResponse(ChatClient.ChatClientRequestSpec chatClientRequestSpec) throws Exception {
        return isUseStream() ? getFlowChatResponse(chatClientRequestSpec.stream().chatResponse()) : chatClientRequestSpec.call().chatResponse();
    }

    public static boolean isUseStream() {
        return Convert.toBool(SpringContextUtil.getProperty("custom.useStream"));
    }

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
        var latch = new CountDownLatch(1);
        final ChatResponse[] result = new ChatResponse[1];
        Disposable disposable = null;

        try {
            Flux<ChatResponse> res = new MyMessageAggregator().aggregate(responseFlux,
                    chatResponse -> {
                log.info("111222");
                result[0] = chatResponse;
                latch.countDown();
            });
            disposable = res.subscribe();
        } catch (Exception e) {
            log.error("getFlowChatResponse error", e);
            latch.countDown();
            throw e;
        } finally {
            latch.await();
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }
        return result[0];
    }
}
