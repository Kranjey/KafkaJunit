package kafka.tests.data;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import com.example.kafka.DTO.OrderEvent;
import com.example.kafka.DTO.OrderStatus;

public class TestDataFactory {

    public static Stream<Arguments> providePositiveCases() {
        return Stream.of(
            Arguments.of("POS-001", 100.50, "USD", OrderStatus.ACCEPTED, "Standard"),
            Arguments.of("POS-002", 0.01, "EUR", OrderStatus.ACCEPTED, "MIN"),
            Arguments.of("POS-003", 999999.99, "RUB", OrderStatus.ACCEPTED, "MAX")
        );
    }

    public static Stream<Arguments> provideNegativeCases() {
        return Stream.of(
            Arguments.of("NEG-001", 0.0, "USD", OrderStatus.REJECTED, "Zero"),
            Arguments.of("NEG-002", -50.0, "EUR", OrderStatus.REJECTED, "Negative sum"),
            Arguments.of("NEG-003", -0.0001, "RUB", OrderStatus.REJECTED, "Negative float sum")
        );
    }
    }
