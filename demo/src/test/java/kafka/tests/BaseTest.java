package kafka.tests;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.kafka.DTO.OrderEvent;
import com.example.kafka.DTO.OrderResult;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"kafka.tests", "com.example.kafka"})
@SpringBootConfiguration
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) 
public abstract class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    @Container
    public static final KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    @Autowired
    protected KafkaTemplate<String, Object> producerTemplate;

    protected Consumer<String, Object> testConsumer;
    protected String uniqueGroupId;

    protected void sendOrderEvent(String topic, OrderEvent event) {
        log.debug("Sending OrderEvent to {}: {}", topic, event.getOrderId());
        producerTemplate.send(topic, event.getOrderId(), event);
        producerTemplate.flush();
    }

    protected OrderResult waitForOrderResult(String outputTopic, String orderId, Duration timeout) {
        log.debug("Waiting for OrderResult in topic '{}' for orderId={}", outputTopic, orderId);
        
        testConsumer.subscribe(Collections.singletonList(outputTopic));

        OrderResult[] resultHolder = new OrderResult[1];

        await().atMost(timeout)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var records = testConsumer.poll(Duration.ofMillis(100));
                    
                    for (var record : records) {
                        OrderResult result = (OrderResult) record.value();
                        if (orderId.equals(result.getOrderId())) {
                            resultHolder[0] = result;
                            log.debug("✅ Found matching result for orderId={}", orderId);
                            return;
                        }
                    }
                    
                    if (records.isEmpty()) {
                        log.trace("No records polled, waiting...");
                    } else {
                        log.trace("Polled {} records, but none matched orderId={}", 
                                 records.count(), orderId);
                    }
                });

        return resultHolder[0];
    }

    protected OrderResult waitForOrderResult(String outputTopic, String orderId) {
        return waitForOrderResult(outputTopic, orderId, Duration.ofSeconds(10));
    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
    String bootstrapServers = kafkaContainer.getBootstrapServers();
    log.info("Kafka Bootstrap Servers: {}", bootstrapServers);
    
    registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
    registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
    registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
}

    @BeforeEach
    void setupBase() {
        uniqueGroupId = "test-group-" + UUID.randomUUID();

    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, uniqueGroupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(JsonDeserializer.TYPE_MAPPINGS, "orderResult:com.example.kafka.DTO.OrderResult");

        this.testConsumer = new DefaultKafkaConsumerFactory<String, Object>(props).createConsumer();
        
    log.info("Kafka Test Environment Ready: {}", kafkaContainer.getBootstrapServers());
    }
    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            try {
                testConsumer.close(Duration.ofSeconds(5));
                log.info(" Consumer closed successfully");
            } catch (Exception e) {
                log.warn("Error closing consumer: {}", e.getMessage());
            }
        }
    }

    @AfterAll
    static void stopContainer() {
        log.info("Stopping Kafka container...");
        kafkaContainer.stop();
        log.info("Kafka container stopped");
    }
}