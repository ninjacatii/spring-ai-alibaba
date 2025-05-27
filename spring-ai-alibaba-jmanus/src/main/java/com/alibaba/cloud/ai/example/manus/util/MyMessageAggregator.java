package com.alibaba.cloud.ai.example.manus.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.EmptyRateLimit;
import org.springframework.ai.chat.metadata.PromptMetadata;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Slf4j
public class MyMessageAggregator {
    public Flux<AdvisedResponse> aggregateAdvisedResponse(Flux<AdvisedResponse> advisedResponses, Consumer<AdvisedResponse> aggregationHandler) {
        AtomicReference<Map<String, Object>> adviseContext = new AtomicReference(new HashMap());
        return (new org.springframework.ai.chat.model.MessageAggregator()).aggregate(advisedResponses.map((ar) -> {
            ((Map)adviseContext.get()).putAll(ar.adviseContext());
            return ar.response();
        }), (aggregatedChatResponse) -> {
            AdvisedResponse aggregatedAdvisedResponse = AdvisedResponse.builder().response(aggregatedChatResponse).adviseContext((Map)adviseContext.get()).build();
            aggregationHandler.accept(aggregatedAdvisedResponse);
        }).map((cr) -> new AdvisedResponse(cr, (Map)adviseContext.get()));
    }

    public Flux<ChatResponse> aggregate(Flux<ChatResponse> fluxChatResponse, Consumer<ChatResponse> onAggregationComplete) {
        AtomicReference<StringBuilder> messageTextContentRef = new AtomicReference(new StringBuilder());
        AtomicReference<Map<String, Object>> messageMetadataMapRef = new AtomicReference();
        AtomicReference<Map<String, AssistantMessage.ToolCall>> messageToolCallMapRef = new AtomicReference();
        AtomicReference<Map<String, String>> messageToolCallArgsMapRef = new AtomicReference();
        AtomicReference<ChatGenerationMetadata> generationMetadataRef = new AtomicReference(ChatGenerationMetadata.NULL);
        AtomicReference<Integer> metadataUsagePromptTokensRef = new AtomicReference(0);
        AtomicReference<Integer> metadataUsageGenerationTokensRef = new AtomicReference(0);
        AtomicReference<Integer> metadataUsageTotalTokensRef = new AtomicReference(0);
        AtomicReference<PromptMetadata> metadataPromptMetadataRef = new AtomicReference(PromptMetadata.empty());
        AtomicReference<RateLimit> metadataRateLimitRef = new AtomicReference(new EmptyRateLimit());
        AtomicReference<String> metadataIdRef = new AtomicReference("");
        AtomicReference<String> metadataModelRef = new AtomicReference("");
        return fluxChatResponse.doOnSubscribe((subscription) -> {
            messageTextContentRef.set(new StringBuilder());
            messageMetadataMapRef.set(new HashMap());
            messageToolCallMapRef.set(new HashMap<>());
            messageToolCallArgsMapRef.set(new HashMap<>());
            metadataIdRef.set("");
            metadataModelRef.set("");
            metadataUsagePromptTokensRef.set(0);
            metadataUsageGenerationTokensRef.set(0);
            metadataUsageTotalTokensRef.set(0);
            metadataPromptMetadataRef.set(PromptMetadata.empty());
            metadataRateLimitRef.set(new EmptyRateLimit());
        }).doOnNext((chatResponse) -> {
            if (chatResponse.getResult() != null) {
                if (chatResponse.getResult().getMetadata() != null && chatResponse.getResult().getMetadata() != ChatGenerationMetadata.NULL) {
                    generationMetadataRef.set(chatResponse.getResult().getMetadata());
                }

                if (chatResponse.getResult().getOutput().getText() != null) {
                    ((StringBuilder)messageTextContentRef.get()).append(chatResponse.getResult().getOutput().getText());
                }

                if (chatResponse.getResult().getOutput().getMetadata() != null) {
                    ((Map)messageMetadataMapRef.get()).putAll(chatResponse.getResult().getOutput().getMetadata());
                }

                if (chatResponse.getResult().getOutput().getToolCalls() != null) {
                    for (AssistantMessage.ToolCall toolCall: chatResponse.getResult().getOutput().getToolCalls()) {
                        if (messageToolCallMapRef.get().containsKey(toolCall.id())) {
                            messageToolCallArgsMapRef.get().put(toolCall.id(), messageToolCallArgsMapRef.get().get(toolCall.id()) + toolCall.arguments());
                        } else {
                            messageToolCallMapRef.get().put(toolCall.id(), toolCall);
                            messageToolCallArgsMapRef.get().put(toolCall.id(), StrUtil.isBlank(toolCall.arguments()) ? "" : toolCall.arguments());
                        }
                    }
                }
            }

            if (chatResponse.getMetadata() != null) {
                if (chatResponse.getMetadata().getUsage() != null) {
                    Usage usage = chatResponse.getMetadata().getUsage();
                    metadataUsagePromptTokensRef.set(usage.getPromptTokens() > 0 ? usage.getPromptTokens() : (Integer)metadataUsagePromptTokensRef.get());
                    metadataUsageGenerationTokensRef.set(usage.getCompletionTokens() > 0 ? usage.getCompletionTokens() : (Integer)metadataUsageGenerationTokensRef.get());
                    metadataUsageTotalTokensRef.set(usage.getTotalTokens() > 0 ? usage.getTotalTokens() : (Integer)metadataUsageTotalTokensRef.get());
                }

                if (chatResponse.getMetadata().getPromptMetadata() != null && chatResponse.getMetadata().getPromptMetadata().iterator().hasNext()) {
                    metadataPromptMetadataRef.set(chatResponse.getMetadata().getPromptMetadata());
                }

                if (chatResponse.getMetadata().getRateLimit() != null && !(metadataRateLimitRef.get() instanceof EmptyRateLimit)) {
                    metadataRateLimitRef.set(chatResponse.getMetadata().getRateLimit());
                }

                if (StringUtils.hasText(chatResponse.getMetadata().getId())) {
                    metadataIdRef.set(chatResponse.getMetadata().getId());
                }

                if (StringUtils.hasText(chatResponse.getMetadata().getModel())) {
                    metadataModelRef.set(chatResponse.getMetadata().getModel());
                }
            }

        }).doOnComplete(() -> {
            org.springframework.ai.chat.model.MessageAggregator.DefaultUsage usage = new org.springframework.ai.chat.model.MessageAggregator.DefaultUsage((Integer)metadataUsagePromptTokensRef.get(), (Integer)metadataUsageGenerationTokensRef.get(), (Integer)metadataUsageTotalTokensRef.get());
            ChatResponseMetadata chatResponseMetadata = ChatResponseMetadata.builder().id((String)metadataIdRef.get()).model((String)metadataModelRef.get()).rateLimit((RateLimit)metadataRateLimitRef.get()).usage(usage).promptMetadata((PromptMetadata)metadataPromptMetadataRef.get()).build();
            onAggregationComplete.accept(new ChatResponse(List.of(new Generation(new AssistantMessage(((StringBuilder)messageTextContentRef.get()).toString(), (Map)messageMetadataMapRef.get(), getToolCalls(messageToolCallMapRef, messageToolCallArgsMapRef)), (ChatGenerationMetadata)generationMetadataRef.get())), chatResponseMetadata));
            messageTextContentRef.set(new StringBuilder());
            messageMetadataMapRef.set(new HashMap());
            messageToolCallMapRef.set(new HashMap<>());
            messageToolCallArgsMapRef.set(new HashMap<>());
            metadataIdRef.set("");
            metadataModelRef.set("");
            metadataUsagePromptTokensRef.set(0);
            metadataUsageGenerationTokensRef.set(0);
            metadataUsageTotalTokensRef.set(0);
            metadataPromptMetadataRef.set(PromptMetadata.empty());
            metadataRateLimitRef.set(new EmptyRateLimit());
        }).doOnError((e) -> log.error("Aggregation Error", e));
    }

    private List<AssistantMessage.ToolCall> getToolCalls(AtomicReference<Map<String, AssistantMessage.ToolCall>> messageToolCallMapRef, AtomicReference<Map<String, String>> messageToolCallArgsMapRef) {
        var list = new ArrayList<AssistantMessage.ToolCall>();
        for (Map.Entry<String, AssistantMessage.ToolCall> entry: messageToolCallMapRef.get().entrySet()) {
            String id = entry.getKey();
            AssistantMessage.ToolCall toolCall = entry.getValue();
            list.add(new AssistantMessage.ToolCall(id, toolCall.type(), toolCall.name(), messageToolCallArgsMapRef.get().get(id)));
        }
        return list;
    }
}
