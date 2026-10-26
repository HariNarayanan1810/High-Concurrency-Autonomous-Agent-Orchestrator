package com.zaalima.orchestrator.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaalima.orchestrator.config.OrchestrationProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OrchestrationPropertiesTest {

    @Test
    void timeoutCannotBeZeroNegativeOrUnbounded() {
        for (Duration timeout : new Duration[]{Duration.ZERO, Duration.ofSeconds(-1), Duration.ofDays(2)}) {
            assertThatThrownBy(() -> new OrchestrationProperties(true, timeout, 1, Duration.ofHours(1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void fanOutMustBeBoundedAndPositive() {
        for (int maximum : new int[]{0, -1, 65}) {
            assertThatThrownBy(() -> new OrchestrationProperties(true, Duration.ofSeconds(1), maximum, Duration.ofHours(1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void retentionMustBePositive() {
        assertThatThrownBy(() -> new OrchestrationProperties(false, Duration.ofSeconds(1), 1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
