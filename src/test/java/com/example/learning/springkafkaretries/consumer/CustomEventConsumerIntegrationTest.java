package com.example.learning.springkafkaretries.consumer;

import com.example.learning.springkafkaretries.handler.CustomEventHandler;
import com.example.learning.springkafkaretries.handler.exceptions.CustomNonRetryableException;
import com.example.learning.springkafkaretries.handler.exceptions.CustomRetryableException;
import com.example.learning.springkafkaretries.model.CustomEvent;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@EnableKafka
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(partitions = 1,
        brokerProperties = {"listeners=" + "${kafka.broker-properties.listeners}",
                "port=" + "${kafka.broker-properties.port}"},
        controlledShutdown = true,
        topics = {"test", "test-retry-0", "test-retry-1", "test-retry-2", "test-dlt"}
)
@ActiveProfiles("test")
class CustomEventConsumerIntegrationTest {

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, CustomEvent> testKafkaTemplate;

    @Value("${retry.attempts}")
    private int maxRetries;
    @MockBean
    CustomEventHandler customEventHandler;


    @BeforeEach
    void setUp() {
        //wait for partitions to be assigned
        registry.getListenerContainers().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
        log.info("Partitioned Assigned. Starting tests");
    }

    @AfterEach
    void tearDown() {
        embeddedKafkaBroker.destroy();
    }

    @Test
    void test_should_not_retry_if_consumption_is_successful() throws ExecutionException, InterruptedException {
        CustomEvent event = new CustomEvent("Hello");
        // GIVEN
        doNothing().when(customEventHandler).handleEvent(any(CustomEvent.class));

        // WHEN
        testKafkaTemplate.send("test", event).get();

        // THEN
        verify(customEventHandler, timeout(2000).times(1)).handleEvent(any(CustomEvent.class));
        verify(customEventHandler, timeout(2000).times(0)).handleEventFromDlt(any(CustomEvent.class));
    }

    @Test
    void test_should_retry_maximum_times_and_publish_to_dlt_if_retryable_exception_raised() throws ExecutionException, InterruptedException {
        CustomEvent event = new CustomEvent("Hello");
        // GIVEN
        doThrow(CustomRetryableException.class).when(customEventHandler).handleEvent(any(CustomEvent.class));

        // WHEN
        testKafkaTemplate.send("test", event).get();

        // THEN
        verify(customEventHandler, timeout(10000).times(maxRetries)).handleEvent(any(CustomEvent.class));
        verify(customEventHandler, timeout(2000).times(1)).handleEventFromDlt(any(CustomEvent.class));
    }

    @Test
    void test_should_not_retry_if_non_retryable_exception_raised() throws ExecutionException, InterruptedException {
        CustomEvent event = new CustomEvent("Hello");
        // GIVEN
        doThrow(CustomNonRetryableException.class).when(customEventHandler).handleEvent(any(CustomEvent.class));

        // WHEN
        testKafkaTemplate.send("test", event).get();

        // THEN
        verify(customEventHandler, timeout(2000).times(1)).handleEvent(any(CustomEvent.class));
        verify(customEventHandler, timeout(2000).times(0)).handleEventFromDlt(any(CustomEvent.class));
    }

    @Test
    void test_should_retry_until_retryable_exception_is_resolved_by_non_retryable_exception() throws ExecutionException,
            InterruptedException {
        CustomEvent event = new CustomEvent("Hello");
        // GIVEN
        doThrow(CustomRetryableException.class).doThrow(CustomNonRetryableException.class).when(customEventHandler).handleEvent(any(CustomEvent.class));

        // WHEN
        testKafkaTemplate.send("test", event).get();

        // THEN
        verify(customEventHandler, timeout(10000).times(2)).handleEvent(any(CustomEvent.class));
        verify(customEventHandler, timeout(2000).times(0)).handleEventFromDlt(any(CustomEvent.class));
    }

    @Test
    void test_should_retry_until_retryable_exception_is_resolved_by_successful_consumption() throws ExecutionException,
            InterruptedException {
        CustomEvent event = new CustomEvent("Hello");
        // GIVEN
        doThrow(CustomRetryableException.class).doNothing().when(customEventHandler).handleEvent(any(CustomEvent.class));

        // WHEN
        testKafkaTemplate.send("test", event).get();

        // THEN
        verify(customEventHandler, timeout(10000).times(2)).handleEvent(any(CustomEvent.class));
        verify(customEventHandler, timeout(2000).times(0)).handleEventFromDlt(any(CustomEvent.class));
    }
}