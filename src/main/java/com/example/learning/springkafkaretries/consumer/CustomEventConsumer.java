package com.example.learning.springkafkaretries.consumer;

import com.example.learning.springkafkaretries.handler.CustomEventHandler;
import com.example.learning.springkafkaretries.handler.exceptions.CustomRetryableException;
import com.example.learning.springkafkaretries.model.CustomEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomEventConsumer {

    private final CustomEventHandler handler;

    @RetryableTopic(attempts = "${retry.attempts}",
            backoff = @Backoff(
                    delayExpression = "${retry.delay}",
                    multiplierExpression = "${retry.delay.multiplier}"
            ),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = FAIL_ON_ERROR,
            autoStartDltHandler = "true",
            autoCreateTopics = "false",
            include = {CustomRetryableException.class})
    @KafkaListener(topics = "${topic}", id = "${default-consumer-group:default}")
    public void consume(CustomEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            log.info("Received event on topic {}", topic);
            handler.handleEvent(event);
        } catch (Exception e) {
            log.error("Error occurred while processing event", e);
            throw e;
        }
    }

    @DltHandler
    public void listenOnDlt(@Payload CustomEvent event) {
        log.error("Received event on dlt.");
        handler.handleEventFromDlt(event);
    }

}
