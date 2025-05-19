package com.alibaba.cloud.ai.example.manus.llm;

import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class LlmTestServiceTest {
    private static final Logger log = LoggerFactory.getLogger(LlmTestServiceTest.class);
    @Autowired
    LlmTestService llmTestService;

    @Test
    public void test() {
        log.info(llmTestService.chat("用两百个单词左右介绍你是谁。"));
    }

}