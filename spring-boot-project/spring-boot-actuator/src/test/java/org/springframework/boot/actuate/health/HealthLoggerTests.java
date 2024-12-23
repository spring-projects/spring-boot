package org.springframework.boot.actuate.health;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class HealthLoggerTests {

    private static final Log logger = LogFactory.getLog(HealthLogger.class);

    @Test
    void logExceptionIfPresentShouldLogException(CapturedOutput output) {
        Exception exception = new Exception("Test exception");
        HealthLogger.logExceptionIfPresent(exception);
        assertThat(output).contains("Test exception");
    }

    @Test
    void logExceptionIfPresentShouldNotLogWhenNoException(CapturedOutput output) {
        HealthLogger.logExceptionIfPresent(null);
        assertThat(output).doesNotContain("Health check failed");
    }

    @Test
    void logExceptionIfPresentShouldLogDefaultMessageWhenNoMessage(CapturedOutput output) {
        Exception exception = new Exception();
        HealthLogger.logExceptionIfPresent(exception);
        assertThat(output).contains("Health check failed");
    }
}
