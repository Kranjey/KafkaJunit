package kafka.tests;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.kafka.DTO.OrderEvent;
import com.example.kafka.DTO.OrderResult;
import com.example.kafka.DTO.OrderStatus;

import kafka.tests.data.TestDataFactory;

import java.time.Duration;
import java.util.Collections;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes = BaseTest.class)
@DisplayName("Негативные сценарии обработки заказов (Валидация)")
public class NegativeOrderTests extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(NegativeOrderTests.class);
    private static final String INPUT_TOPIC = "orders.created";
    private static final String OUTPUT_TOPIC = "orders.processed";

    @ParameterizedTest(name = "{index}: {5}")
    @MethodSource("kafka.tests.data.TestDataFactory#provideNegativeCases")
    void shouldRejectInvalidAmounts(String orderId, double amount, String currency, 
                                    OrderStatus expectedStatus, String description) {
        log.info(">>> Start Negative Test: {}", description);
        OrderEvent event = new OrderEvent(orderId, amount, currency);
        sendOrderEvent(INPUT_TOPIC, event);
        OrderResult result = waitForOrderResult(OUTPUT_TOPIC, orderId);
        assertNotNull(result, "Result should not be null");
        assertEquals(orderId, result.getOrderId(), "Order ID mismatch");
        assertEquals(OrderStatus.REJECTED, result.getStatus(), 
                     "Status should be REJECTED for: " + description);
        assertTrue(result.getReason().toLowerCase().contains("amount") || 
                   result.getReason().toLowerCase().contains("positive"),
                   "Reason should mention amount validation: " + result.getReason());
        
        log.info("<<< Test Passed (Correctly Rejected): {}", description);
    }
}